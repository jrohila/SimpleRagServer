package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.github.jrohila.simpleragserver.domain.DocumentEntity.ProcessingState;
import io.github.jrohila.simpleragserver.repository.DocumentContentStore;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Service
public class DocumentService {

    @Value("${documents.index-name}")
    private String baseIndexName;

    @Autowired
    private OpenSearchClient openSearchClient;

    private final DocumentContentStore contentStore;
    private final ChunkService chunkService;
    private final ApplicationEventPublisher events;

    public DocumentService(
            DocumentContentStore contentStore,
            ChunkService chunkService,
            ApplicationEventPublisher events
    ) {
        this.contentStore = contentStore;
        this.chunkService = chunkService;
        this.events = events;
    }

    public java.util.List<DocumentEntity> listDocuments(int page, int size) {
        try {
            var resp = openSearchClient.search(s -> s
                .index(baseIndexName)
                .from(page * size)
                .size(size)
                .query(q -> q.matchAll(m -> m))
            , DocumentEntity.class);
            java.util.List<DocumentEntity> results = new java.util.ArrayList<>();
            for (var hit : resp.hits().hits()) {
                results.add(hit.source());
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list documents", e);
        }
    }

    public Optional<DocumentEntity> getById(String id) {
        try {
            var resp = openSearchClient.get(g -> g.index(baseIndexName).id(id), DocumentEntity.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get document by id", e);
        }
    }

    /**
     * Alias for getById for compatibility with repository-style naming.
     */
    public Optional<DocumentEntity> findById(String id) {
        return getById(id);
    }

    public DocumentEntity uploadDocument(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String hash = DigestUtils.sha256Hex(file.getInputStream());
        if (findByHash(hash).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document with the same hash already exists");
        }

    String now = java.time.Instant.now().toString();
        DocumentEntity doc = new DocumentEntity();
        // Only set fields that exist on DocumentEntity
        doc.setHash(hash);
    // Set id to a random UUID
    doc.setId(java.util.UUID.randomUUID().toString());
        if (file.getOriginalFilename() != null) {
            doc.setOriginalFilename(file.getOriginalFilename());
        }
        if (file.getContentType() != null) {
            doc.setMimeType(file.getContentType());
        }
        // content length may be set by the content store, but we can prefill
        doc.setContentLen(file.getSize());

    doc.setState(DocumentEntity.ProcessingState.OPEN);
    doc.setCreatedTime(now);
    doc.setUpdatedTime(now);

        // Persist content and metadata
        contentStore.setContent(doc, file.getInputStream());
        indexDocument(doc);
            try {
                events.publishEvent(new io.github.jrohila.simpleragserver.service.events.DocumentSavedEvent(doc.getId()));
            } catch (Exception ignore) {
            }
        return doc;
    }

    public DocumentEntity updateDocument(String id, MultipartFile file, String language) throws IOException {
    String now = java.time.Instant.now().toString();
        DocumentEntity doc = getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String newHash = DigestUtils.sha256Hex(file.getInputStream());
        if (findByHashExcludingId(newHash, id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another document with the same hash exists");
        }

        // Update fields that exist on DocumentEntity
        doc.setHash(newHash);
        if (file.getOriginalFilename() != null) {
            doc.setOriginalFilename(file.getOriginalFilename());
        }
        if (file.getContentType() != null) {
            doc.setMimeType(file.getContentType());
        }
        doc.setContentLen(file.getSize());

        contentStore.setContent(doc, file.getInputStream());
        if (doc.getCreatedTime() == null) {
            doc.setCreatedTime(now);
        }
        doc.setUpdatedTime(now);
        indexDocument(doc);
            try {
                events.publishEvent(new io.github.jrohila.simpleragserver.service.events.DocumentSavedEvent(doc.getId()));
            } catch (Exception ignore) {
            }
        return doc;
    }

    public void deleteDocument(String id) {
        if (!existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        // Cascade delete chunks by documentId if your repository supports it
        try {
            chunkService.deleteByDocumentId(id);
        } catch (Exception ignore) {
        }
        try {
            openSearchClient.delete(d -> d.index(baseIndexName).id(id));
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete document", e);
        }
    }

    public void deleteAllDocuments() {
        // Delete all chunks before deleting documents
        try {
            chunkService.deleteAll();
        } catch (Exception ignore) {
        }
        try {
            openSearchClient.deleteByQuery(d -> d
                .index(baseIndexName)
                .query(q -> q.matchAll(m -> m))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all documents", e);
        }
    }

    public DocumentEntity updateProcessingState(String documentId, ProcessingState state) {
        if (documentId == null || documentId.isBlank() || state == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "documentId and state are required");
        }
        DocumentEntity doc = getById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        doc.setState(state);
        indexDocument(doc);
        return doc;
    }

    // --- OpenSearchClient helper methods ---

    private void indexDocument(DocumentEntity doc) {
        try {
            openSearchClient.index(i -> i
                .index(baseIndexName)
                .id(doc.getId())
                .document(doc)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to index document", e);
        }
    }

    private Optional<DocumentEntity> findByHash(String hash) {
        try {
            var resp = openSearchClient.search(s -> s
                .index(baseIndexName)
                .size(1)
                .query(q -> q.term(t -> t.field("hash").value(org.opensearch.client.opensearch._types.FieldValue.of(hash))))
            , DocumentEntity.class);
            if (!resp.hits().hits().isEmpty()) {
                return Optional.of(resp.hits().hits().get(0).source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to search by hash", e);
        }
    }

    private Optional<DocumentEntity> findByHashExcludingId(String hash, String excludeId) {
        try {
            var resp = openSearchClient.search(s -> s
                .index(baseIndexName)
                .size(2)
                .query(q -> q.bool(b -> b
                    .must(q2 -> q2.term(t -> t.field("hash").value(org.opensearch.client.opensearch._types.FieldValue.of(hash))))
                ))
            , DocumentEntity.class);
            return resp.hits().hits().stream()
                .map(h -> h.source())
                .filter(d -> !d.getId().equals(excludeId))
                .findFirst();
        } catch (Exception e) {
            throw new RuntimeException("Failed to search by hash (excluding id)", e);
        }
    }

    private boolean existsById(String id) {
        try {
            var resp = openSearchClient.get(g -> g.index(baseIndexName).id(id), DocumentEntity.class);
            return resp.found();
        } catch (Exception e) {
            throw new RuntimeException("Failed to check existence by id", e);
        }
    }
}

package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.github.jrohila.simpleragserver.domain.DocumentEntity.ProcessingState;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class DocumentService {

    @Autowired
    private OpenSearchClient openSearchClient;

    private final DocumentContentStore contentStore;
    private final ChunkService chunkService;
    private final ApplicationEventPublisher events;
    private final IndicesManager indicesManager;

    public DocumentService(
            DocumentContentStore contentStore,
            ChunkService chunkService,
            IndicesManager indicesManager,
            ApplicationEventPublisher events
    ) {
        this.contentStore = contentStore;
        this.chunkService = chunkService;
        this.indicesManager = indicesManager;
        this.events = events;
    }

    public List<DocumentEntity> listDocuments(String collectionId, int page, int size) {
        try {
            String indiceName = indicesManager.createIfNotExist(collectionId, DocumentEntity.class);

            var resp = openSearchClient.search(s -> s
                    .index(indiceName)
                    .from(page * size)
                    .size(size)
                    .query(q -> q.matchAll(m -> m)),
                    DocumentEntity.class);
            List<DocumentEntity> results = new ArrayList<>();
            for (var hit : resp.hits().hits()) {
                results.add(hit.source());
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list documents", e);
        }
    }

    public Optional<DocumentEntity> getById(String collectionId, String id) {
        try {
            String indiceName = indicesManager.createIfNotExist(collectionId, DocumentEntity.class);

            var resp = openSearchClient.get(g -> g.index(indiceName).id(id), DocumentEntity.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get document by id", e);
        }
    }

    public DocumentEntity uploadDocument(String collectionId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String hash = DigestUtils.sha256Hex(file.getInputStream());
        if (findByHash(collectionId, hash).isPresent()) {
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
        indexDocument(collectionId, doc);
        try {
            events.publishEvent(new io.github.jrohila.simpleragserver.service.events.DocumentSavedEvent(collectionId, doc.getId()));
        } catch (Exception ignore) {
        }
        return doc;
    }

    public DocumentEntity updateDocument(String collectionId, String id, MultipartFile file, String language) throws IOException {
        String now = java.time.Instant.now().toString();
        DocumentEntity doc = getById(collectionId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String newHash = DigestUtils.sha256Hex(file.getInputStream());
        if (findByHashExcludingId(collectionId, newHash, id).isPresent()) {
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
        indexDocument(collectionId, doc);
        try {
            events.publishEvent(new io.github.jrohila.simpleragserver.service.events.DocumentSavedEvent(collectionId, doc.getId()));
        } catch (Exception ignore) {
        }
        return doc;
    }

    public void deleteDocument(String collectionId, String id) {
        // Cascade delete chunks by documentId if your repository supports it
        try {
            String incideName = indicesManager.createIfNotExist(collectionId, DocumentEntity.class);

            if (!existsById(collectionId, id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
            }

            chunkService.deleteByDocumentId(collectionId, id);

            openSearchClient.delete(d -> d.index(incideName).id(id));
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete document", e);
        }
    }

    public void deleteAllDocuments(String collectionId) {
        // Delete all chunks before deleting documents
        try {
            String incideName = indicesManager.createIfNotExist(collectionId, DocumentEntity.class);

            chunkService.deleteAll(collectionId);

            openSearchClient.deleteByQuery(d -> d
                    .index(incideName)
                    .query(q -> q.matchAll(m -> m))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all documents", e);
        }
    }

    public DocumentEntity updateProcessingState(String collectionId, String documentId, ProcessingState state) {
        if (documentId == null || documentId.isBlank() || state == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "documentId and state are required");
        }
        DocumentEntity doc = getById(collectionId, documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        doc.setState(state);
        indexDocument(collectionId, doc);
        return doc;
    }

    // --- OpenSearchClient helper methods ---
    private void indexDocument(String collectionId, DocumentEntity doc) {
        try {
            String incideName = indicesManager.createIfNotExist(collectionId, DocumentEntity.class);

            openSearchClient.index(i -> i
                    .index(incideName)
                    .id(doc.getId())
                    .document(doc)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to index document", e);
        }
    }

    private Optional<DocumentEntity> findByHash(String collectionId, String hash) {
        try {
            String incideName = indicesManager.createIfNotExist(collectionId, DocumentEntity.class);

            var resp = openSearchClient.search(s -> s
                    .index(incideName)
                    .size(1)
                    .query(q -> q.term(t -> t.field("hash").value(org.opensearch.client.opensearch._types.FieldValue.of(hash)))),
                    DocumentEntity.class);
            if (!resp.hits().hits().isEmpty()) {
                return Optional.of(resp.hits().hits().get(0).source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to search by hash", e);
        }
    }

    private Optional<DocumentEntity> findByHashExcludingId(String collectionId, String hash, String excludeId) {
        try {
            String incideName = indicesManager.createIfNotExist(collectionId, DocumentEntity.class);

            var resp = openSearchClient.search(s -> s
                    .index(incideName)
                    .size(2)
                    .query(q -> q.bool(b -> b
                    .must(q2 -> q2.term(t -> t.field("hash").value(org.opensearch.client.opensearch._types.FieldValue.of(hash))))
            )),
                    DocumentEntity.class);
            return resp.hits().hits().stream()
                    .map(h -> h.source())
                    .filter(d -> !d.getId().equals(excludeId))
                    .findFirst();
        } catch (Exception e) {
            throw new RuntimeException("Failed to search by hash (excluding id)", e);
        }
    }

    private boolean existsById(String collectionId, String id) {
        try {
            String incideName = indicesManager.createIfNotExist(collectionId, DocumentEntity.class);

            var resp = openSearchClient.get(g -> g.index(incideName).id(id), DocumentEntity.class);
            return resp.found();
        } catch (Exception e) {
            throw new RuntimeException("Failed to check existence by id", e);
        }
    }
}

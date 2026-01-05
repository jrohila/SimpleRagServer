package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.github.jrohila.simpleragserver.domain.DocumentEntity.ProcessingState;
import io.github.jrohila.simpleragserver.service.EventPublisherService;
import io.github.jrohila.simpleragserver.service.FileStorageService;
import org.apache.commons.codec.digest.DigestUtils;
import io.micronaut.http.HttpStatus;
import jakarta.inject.Singleton;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.exceptions.HttpStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opensearch.client.opensearch.OpenSearchClient;

@Singleton
public class DocumentService {

    private final OpenSearchClient openSearchClient;
    private final EventPublisherService eventPublisherService;
    private final FileStorageService fileStorageService;
    private final ChunkService chunkService;
    private final IndicesManager indicesManager;

    public DocumentService(
            OpenSearchClient openSearchClient,
            EventPublisherService eventPublisherService,
            FileStorageService fileStorageService,
            ChunkService chunkService,
            IndicesManager indicesManager
    ) {
        this.openSearchClient = openSearchClient;
        this.eventPublisherService = eventPublisherService;
        this.fileStorageService = fileStorageService;
        this.chunkService = chunkService;
        this.indicesManager = indicesManager;
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

    public List<DocumentEntity> findByState(String collectionId, ProcessingState state, int page, int size) {
        try {
            String indiceName = indicesManager.createIfNotExist(collectionId, DocumentEntity.class);

            var resp = openSearchClient.search(s -> s
                    .index(indiceName)
                    .from(page * size)
                    .size(size)
                    .query(q -> q.term(t -> t.field("state").value(org.opensearch.client.opensearch._types.FieldValue.of(state.name())))),
                    DocumentEntity.class);

            List<DocumentEntity> results = new ArrayList<>();
            for (var hit : resp.hits().hits()) {
                results.add(hit.source());
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find documents by state", e);
        }
    }

    public DocumentEntity uploadDocument(String collectionId, CompletedFileUpload file) throws IOException {
        if (file == null || file.getSize() <= 0) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String hash = DigestUtils.sha256Hex(file.getInputStream());
        if (findByHash(collectionId, hash).isPresent()) {
            throw new HttpStatusException(HttpStatus.CONFLICT, "Document with the same hash already exists");
        }

        String now = java.time.Instant.now().toString();
        DocumentEntity doc = new DocumentEntity();
        // Only set fields that exist on DocumentEntity
        doc.setHash(hash);
        // Set id to a random UUID
        String docId = java.util.UUID.randomUUID().toString();
        doc.setId(docId);
        doc.setContentId(docId); // Use same ID for content storage
        if (file.getFilename() != null) {
            doc.setOriginalFilename(file.getFilename());
        }
        if (file.getContentType() != null) {
            doc.setMimeType(file.getContentType().toString());
        }
        // content length may be set by the content store, but we can prefill
        doc.setContentLen(file.getSize());

        doc.setState(DocumentEntity.ProcessingState.OPEN);
        doc.setCreatedTime(now);
        doc.setUpdatedTime(now);

        // Persist content and metadata
        fileStorageService.setContent(doc, file.getInputStream());
        indexDocument(collectionId, doc);

        this.eventPublisherService.publishDocumentUploadEvent(collectionId, doc.getId());
        
        return doc;
    }

    public DocumentEntity updateDocument(String collectionId, String id, CompletedFileUpload file, String language) throws IOException {
        String now = java.time.Instant.now().toString();
        DocumentEntity doc = getById(collectionId, id).orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (file == null || file.getSize() <= 0) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String newHash = DigestUtils.sha256Hex(file.getInputStream());
        if (findByHashExcludingId(collectionId, newHash, id).isPresent()) {
            throw new HttpStatusException(HttpStatus.CONFLICT, "Another document with the same hash exists");
        }

        // Update fields that exist on DocumentEntity
        doc.setHash(newHash);
        if (file.getFilename() != null) {
            doc.setOriginalFilename(file.getFilename());
        }
        if (file.getContentType() != null) {
            doc.setMimeType(file.getContentType().toString());
        }
        doc.setContentLen(file.getSize());

        fileStorageService.setContent(doc, file.getInputStream());
        if (doc.getCreatedTime() == null) {
            doc.setCreatedTime(now);
        }
        doc.setUpdatedTime(now);
        indexDocument(collectionId, doc);

        this.eventPublisherService.publishDocumentUploadEvent(collectionId, doc.getId());
        
        return doc;
    }

    public void deleteDocument(String collectionId, String id) {
        // Cascade delete chunks by documentId if your repository supports it
        try {
            String incideName = indicesManager.createIfNotExist(collectionId, DocumentEntity.class);

            if (!existsById(collectionId, id)) {
                throw new HttpStatusException(HttpStatus.NOT_FOUND, "Document not found");
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
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "documentId and state are required");
        }
        DocumentEntity doc = getById(collectionId, documentId)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Document not found"));
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

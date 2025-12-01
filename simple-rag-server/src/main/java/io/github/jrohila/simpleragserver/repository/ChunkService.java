package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.client.EmbeddingClientFactory;
import io.github.jrohila.simpleragserver.domain.ChunkEntity;
// Removed ChunkRepository import
import org.apache.commons.codec.digest.DigestUtils;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// Removed Spring Data imports
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ChunkService {

    @Autowired
    private IndicesManager indicesManager;

    @Autowired
    private OpenSearchClient openSearchClient;

    private final int embeddingDim;

    @Autowired
    public ChunkService(@Value("${chunks.dimension-size}") int embeddingDim) {
        this.embeddingDim = embeddingDim;
    }

    @Autowired
    private EmbeddingClientFactory embedService;
    
    public ChunkEntity create(String collectionId, ChunkEntity chunk) {
        String now = java.time.Instant.now().toString();
        // Compute hash from text + sectionTitle
        String newHash = computeHash(chunk.getText(), chunk.getSectionTitle());
        chunk.setHash(newHash);
        // If id not provided, derive a stable one using documentId + hash
        if ((chunk.getId() == null || chunk.getId().isBlank()) && chunk.getDocumentId() != null) {
            chunk.setId(chunk.getDocumentId() + ":" + newHash);
        }
        validateEmbedding(chunk);
        // Check for existing chunk with same hash
        var existing = findFirstByHash(collectionId, newHash);
        if (existing.isPresent()) {
            throw new IllegalStateException("Chunk with the same hash already exists");
        }
        chunk.setCreated(now);
        chunk.setModified(now);
        // Index the chunk
        try {
            String indexName = indicesManager.createIfNotExist(collectionId, ChunkEntity.class);

            openSearchClient.index(i -> i
                    .index(indexName)
                    .id(chunk.getId())
                    .document(chunk)
            );
            return chunk;
        } catch (Exception e) {
            throw new RuntimeException("Failed to index chunk", e);
        }
    }

    public Optional<ChunkEntity> getById(String collectionId, String id) {
        try {
            String indexName = indicesManager.createIfNotExist(collectionId, ChunkEntity.class);

            var resp = openSearchClient.get(g -> g.index(indexName).id(id), ChunkEntity.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get chunk by id", e);
        }
    }

    public java.util.List<ChunkEntity> list(String collectionId, int page, int size, String documentId) {
        try {
            String indexName = indicesManager.createIfNotExist(collectionId, ChunkEntity.class);

            var builder = org.opensearch.client.opensearch.core.SearchRequest.of(s -> s
                    .index(indexName)
                    .from(page * size)
                    .size(size)
                    .query(q -> {
                        if (documentId != null && !documentId.isEmpty()) {
                            return q.term(t -> t.field("documentId").value(org.opensearch.client.opensearch._types.FieldValue.of(documentId)));
                        } else {
                            return q.matchAll(m -> m);
                        }
                    })
            );
            var resp = openSearchClient.search(builder, ChunkEntity.class);
            java.util.List<ChunkEntity> results = new java.util.ArrayList<>();
            for (var hit : resp.hits().hits()) {
                results.add(hit.source());
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list chunks", e);
        }
    }

    public Optional<ChunkEntity> update(String collectionId, String id, ChunkEntity chunk) {
        String now = java.time.Instant.now().toString();
        // Check if exists
        if (getById(collectionId, id).isEmpty()) {
            return Optional.empty();
        }
        // Compute new hash from updated content
        String newHash = computeHash(chunk.getText(), chunk.getSectionTitle());
        validateEmbedding(chunk);
        var existingWithHash = findFirstByHash(collectionId, newHash);
        if (existingWithHash.isPresent() && !existingWithHash.get().getId().equals(id)) {
            throw new IllegalStateException("Another chunk with the same hash exists");
        }
        chunk.setId(id);
        chunk.setHash(newHash);
        chunk.setEmbedding(this.embedService.getDefaultClient().embedAsList(chunk.getText()));
        if (chunk.getCreated() == null) {
            chunk.setCreated(now);
        }
        chunk.setModified(now);
        try {
            String indexName = indicesManager.createIfNotExist(collectionId, ChunkEntity.class);

            openSearchClient.index(i -> i
                    .index(indexName)
                    .id(id)
                    .document(chunk)
            );
            return Optional.of(chunk);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update chunk", e);
        }
    }

    public boolean deleteById(String collectionId, String id) {
        try {
            String indexName = indicesManager.createIfNotExist(collectionId, ChunkEntity.class);

            var resp = openSearchClient.delete(d -> d.index(indexName).id(id));
            return resp.result().jsonValue().equalsIgnoreCase("deleted");
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete chunk by id", e);
        }
    }

    public void deleteAll(String collectionId) {
        try {
            String indexName = indicesManager.createIfNotExist(collectionId, ChunkEntity.class);

            openSearchClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q.matchAll(m -> m))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all chunks", e);
        }
    }

    public void deleteByDocumentId(String collectionId, String documentId) {
        try {
            String indexName = indicesManager.createIfNotExist(collectionId, ChunkEntity.class);

            openSearchClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q.term(t -> t.field("documentId").value(org.opensearch.client.opensearch._types.FieldValue.of(documentId))))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete chunks by documentId", e);
        }
    }

    // Helper: findFirstByHash using OpenSearchClient
    private Optional<ChunkEntity> findFirstByHash(String collectionId, String hash) {
        try {
            String indexName = indicesManager.createIfNotExist(collectionId, ChunkEntity.class);

            var resp = openSearchClient.search(s -> s
                    .index(indexName)
                    .size(1)
                    .query(q -> q.term(t -> t.field("hash").value(org.opensearch.client.opensearch._types.FieldValue.of(hash)))),
                    ChunkEntity.class);
            if (!resp.hits().hits().isEmpty()) {
                return Optional.of(resp.hits().hits().get(0).source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to search by hash", e);
        }
    }

    private void validateEmbedding(ChunkEntity chunk) {
        var emb = chunk.getEmbedding();
        if (emb == null) {
            throw new IllegalArgumentException("Chunk embedding is required");
        }
        if (emb.size() != embeddingDim) {
            throw new IllegalArgumentException("Embedding dimension mismatch: expected " + embeddingDim + " but got " + emb.size());
        }
        for (Float v : emb) {
            if (v == null || v.isNaN() || v.isInfinite()) {
                throw new IllegalArgumentException("Embedding contains invalid values");
            }
        }
    }

    private String computeHash(String text, String sectionTitle) {
        String t = text == null ? "" : text;
        String s = sectionTitle == null ? "" : sectionTitle;
        // Use a delimiter to avoid accidental collisions from concatenation
        return DigestUtils.sha256Hex(t + "|#|" + s);
    }
}

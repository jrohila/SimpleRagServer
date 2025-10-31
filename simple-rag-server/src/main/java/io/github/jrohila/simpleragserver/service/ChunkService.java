package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
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

    @Value("${chunks.index-name}")
    private String baseIndexName;

    @Autowired
    private OpenSearchClient openSearchClient;

    private final int embeddingDim;

    @Autowired
    public ChunkService(@Value("${chunks.dimension-size}") int embeddingDim) {
        this.embeddingDim = embeddingDim;
    }

    public ChunkEntity create(ChunkEntity chunk) {
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
        var existing = findFirstByHash(newHash);
        if (existing.isPresent()) {
            throw new IllegalStateException("Chunk with the same hash already exists");
        }
        chunk.setCreated(now);
        chunk.setModified(now);
        // Index the chunk
        try {
            openSearchClient.index(i -> i
                .index(baseIndexName)
                .id(chunk.getId())
                .document(chunk)
            );
            return chunk;
        } catch (Exception e) {
            throw new RuntimeException("Failed to index chunk", e);
        }
    }

    public Optional<ChunkEntity> getById(String id) {
        try {
            var resp = openSearchClient.get(g -> g.index(baseIndexName).id(id), ChunkEntity.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get chunk by id", e);
        }
    }

    public java.util.List<ChunkEntity> list(int page, int size, String documentId) {
        try {
            var builder = org.opensearch.client.opensearch.core.SearchRequest.of(s -> s
                .index(baseIndexName)
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

    public Optional<ChunkEntity> update(String id, ChunkEntity chunk) {
    String now = java.time.Instant.now().toString();
        // Check if exists
        if (getById(id).isEmpty()) {
            return Optional.empty();
        }
        // Compute new hash from updated content
        String newHash = computeHash(chunk.getText(), chunk.getSectionTitle());
        validateEmbedding(chunk);
        var existingWithHash = findFirstByHash(newHash);
        if (existingWithHash.isPresent() && !existingWithHash.get().getId().equals(id)) {
            throw new IllegalStateException("Another chunk with the same hash exists");
        }
        chunk.setId(id);
        chunk.setHash(newHash);
        if (chunk.getCreated() == null) {
            chunk.setCreated(now);
        }
        chunk.setModified(now);
        try {
            openSearchClient.index(i -> i
                .index(baseIndexName)
                .id(id)
                .document(chunk)
            );
            return Optional.of(chunk);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update chunk", e);
        }
    }

    public boolean deleteById(String id) {
        try {
            var resp = openSearchClient.delete(d -> d.index(baseIndexName).id(id));
            return resp.result().jsonValue().equalsIgnoreCase("deleted");
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete chunk by id", e);
        }
    }

    public void deleteAll() {
        try {
            openSearchClient.deleteByQuery(d -> d
                .index(baseIndexName)
                .query(q -> q.matchAll(m -> m))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all chunks", e);
        }
    }

    public void deleteByDocumentId(String documentId) {
        try {
            openSearchClient.deleteByQuery(d -> d
                .index(baseIndexName)
                .query(q -> q.term(t -> t.field("documentId").value(org.opensearch.client.opensearch._types.FieldValue.of(documentId))))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete chunks by documentId", e);
        }
    }

    // Helper: findFirstByHash using OpenSearchClient
    private Optional<ChunkEntity> findFirstByHash(String hash) {
        try {
            var resp = openSearchClient.search(s -> s
                .index(baseIndexName)
                .size(1)
                .query(q -> q.term(t -> t.field("hash").value(org.opensearch.client.opensearch._types.FieldValue.of(hash))))
            , ChunkEntity.class);
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

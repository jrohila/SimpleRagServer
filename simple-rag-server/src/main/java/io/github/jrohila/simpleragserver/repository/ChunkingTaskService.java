/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.domain.ChunkingTaskEntity;
import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author Jukka
 */
@Service
public class ChunkingTaskService {
    
    @Autowired
    private OpenSearchClient openSearchClient;
    
    private final IndicesManager indicesManager;
    
    public ChunkingTaskService(IndicesManager indicesManager) {
        this.indicesManager = indicesManager;
    }
    
    /**
     * Create or update a chunking task
     */
    public ChunkingTaskEntity save(ChunkingTaskEntity task) {
        try {
            String indexName = indicesManager.createIfNotExist(ChunkingTaskEntity.class);
            
            // Generate ID if not present
            if (task.getId() == null || task.getId().isBlank()) {
                task.setId(UUID.randomUUID().toString());
            }
            
            // Set timestamps
            String now = java.time.Instant.now().toString();
            if (task.getCreated() == null) {
                task.setCreated(now);
            }
            task.setModified(now);
            
            openSearchClient.index(i -> i
                    .index(indexName)
                    .id(task.getId())
                    .document(task)
            );
            
            return task;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save chunking task", e);
        }
    }
    
    /**
     * Get a chunking task by ID
     */
    public Optional<ChunkingTaskEntity> getById(String id) {
        try {
            String indexName = indicesManager.createIfNotExist(ChunkingTaskEntity.class);
            
            var resp = openSearchClient.get(g -> g.index(indexName).id(id), ChunkingTaskEntity.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get chunking task by id", e);
        }
    }
    
    /**
     * Get all chunking tasks with pagination
     */
    public List<ChunkingTaskEntity> listAll(int page, int size) {
        try {
            String indexName = indicesManager.createIfNotExist(ChunkingTaskEntity.class);
            
            var resp = openSearchClient.search(s -> s
                    .index(indexName)
                    .from(page * size)
                    .size(size)
                    .query(q -> q.matchAll(m -> m)),
                    ChunkingTaskEntity.class);
            
            List<ChunkingTaskEntity> results = new ArrayList<>();
            for (var hit : resp.hits().hits()) {
                results.add(hit.source());
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list chunking tasks", e);
        }
    }
    
    /**
     * Find chunking tasks by collection ID
     */
    public List<ChunkingTaskEntity> findByCollectionId(String collectionId, int page, int size) {
        try {
            String indexName = indicesManager.createIfNotExist(ChunkingTaskEntity.class);
            
            var resp = openSearchClient.search(s -> s
                    .index(indexName)
                    .from(page * size)
                    .size(size)
                    .query(q -> q.term(t -> t.field("collectionId").value(org.opensearch.client.opensearch._types.FieldValue.of(collectionId)))),
                    ChunkingTaskEntity.class);
            
            List<ChunkingTaskEntity> results = new ArrayList<>();
            for (var hit : resp.hits().hits()) {
                results.add(hit.source());
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find chunking tasks by collection ID", e);
        }
    }
    
    /**
     * Find chunking tasks by document ID
     */
    public List<ChunkingTaskEntity> findByDocumentId(String documentId, int page, int size) {
        try {
            String indexName = indicesManager.createIfNotExist(ChunkingTaskEntity.class);
            
            var resp = openSearchClient.search(s -> s
                    .index(indexName)
                    .from(page * size)
                    .size(size)
                    .query(q -> q.term(t -> t.field("documentId").value(org.opensearch.client.opensearch._types.FieldValue.of(documentId)))),
                    ChunkingTaskEntity.class);
            
            List<ChunkingTaskEntity> results = new ArrayList<>();
            for (var hit : resp.hits().hits()) {
                results.add(hit.source());
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find chunking tasks by document ID", e);
        }
    }
    
    /**
     * Find chunking tasks by status
     */
    public List<ChunkingTaskEntity> findByStatus(DocumentEntity.ProcessingState status, int page, int size) {
        try {
            String indexName = indicesManager.createIfNotExist(ChunkingTaskEntity.class);
            
            var resp = openSearchClient.search(s -> s
                    .index(indexName)
                    .from(page * size)
                    .size(size)
                    .query(q -> q.term(t -> t.field("status").value(org.opensearch.client.opensearch._types.FieldValue.of(status.name())))),
                    ChunkingTaskEntity.class);
            
            List<ChunkingTaskEntity> results = new ArrayList<>();
            for (var hit : resp.hits().hits()) {
                results.add(hit.source());
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find chunking tasks by status", e);
        }
    }
    
    /**
     * Find chunking tasks by task ID
     */
    public List<ChunkingTaskEntity> findByTaskId(String taskId, int page, int size) {
        try {
            String indexName = indicesManager.createIfNotExist(ChunkingTaskEntity.class);
            
            var resp = openSearchClient.search(s -> s
                    .index(indexName)
                    .from(page * size)
                    .size(size)
                    .query(q -> q.term(t -> t.field("taskId").value(org.opensearch.client.opensearch._types.FieldValue.of(taskId)))),
                    ChunkingTaskEntity.class);
            
            List<ChunkingTaskEntity> results = new ArrayList<>();
            for (var hit : resp.hits().hits()) {
                results.add(hit.source());
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find chunking tasks by task ID", e);
        }
    }
    
    /**
     * Update the status of a chunking task
     */
    public ChunkingTaskEntity updateStatus(String id, DocumentEntity.ProcessingState status) {
        if (id == null || id.isBlank() || status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id and status are required");
        }
        
        ChunkingTaskEntity task = getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chunking task not found"));
        
        task.setStatus(status);
        return save(task);
    }
    
    /**
     * Delete a chunking task by ID
     */
    public void delete(String id) {
        try {
            String indexName = indicesManager.createIfNotExist(ChunkingTaskEntity.class);
            
            if (!existsById(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chunking task not found");
            }
            
            openSearchClient.delete(d -> d.index(indexName).id(id));
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete chunking task", e);
        }
    }
    
    /**
     * Delete all chunking tasks by collection ID
     */
    public void deleteByCollectionId(String collectionId) {
        try {
            String indexName = indicesManager.createIfNotExist(ChunkingTaskEntity.class);
            
            openSearchClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q.term(t -> t.field("collectionId").value(org.opensearch.client.opensearch._types.FieldValue.of(collectionId))))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete chunking tasks by collection ID", e);
        }
    }
    
    /**
     * Delete all chunking tasks by document ID
     */
    public void deleteByDocumentId(String documentId) {
        try {
            String indexName = indicesManager.createIfNotExist(ChunkingTaskEntity.class);
            
            openSearchClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q.term(t -> t.field("documentId").value(org.opensearch.client.opensearch._types.FieldValue.of(documentId))))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete chunking tasks by document ID", e);
        }
    }
    
    /**
     * Delete all chunking tasks
     */
    public void deleteAll() {
        try {
            String indexName = indicesManager.createIfNotExist(ChunkingTaskEntity.class);
            
            openSearchClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q.matchAll(m -> m))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all chunking tasks", e);
        }
    }
    
    /**
     * Check if a chunking task exists by ID
     */
    private boolean existsById(String id) {
        try {
            String indexName = indicesManager.createIfNotExist(ChunkingTaskEntity.class);
            
            var resp = openSearchClient.get(g -> g.index(indexName).id(id), ChunkingTaskEntity.class);
            return resp.found();
        } catch (Exception e) {
            throw new RuntimeException("Failed to check existence by id", e);
        }
    }
}

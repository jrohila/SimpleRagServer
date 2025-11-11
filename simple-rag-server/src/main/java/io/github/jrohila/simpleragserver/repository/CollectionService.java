package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.domain.CollectionEntity;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.github.jrohila.simpleragserver.repository.DocumentService;

@Service
public class CollectionService {


    @Autowired
    private io.github.jrohila.simpleragserver.repository.IndicesManager indicesManager;

    @Autowired
    private OpenSearchClient openSearchClient;

    @Autowired
    private DocumentService documentService;

    public CollectionEntity create(CollectionEntity collection) {
        if (collection.getId() == null || collection.getId().isBlank()) {
            collection.setId(java.util.UUID.randomUUID().toString());
        }
        String now = java.time.Instant.now().toString();
        collection.setCreated(now);
        collection.setModified(now);
        try {
            String indexName = indicesManager.createIfNotExist(null, CollectionEntity.class);
            openSearchClient.index(IndexRequest.of(i -> i
                .index(indexName)
                .id(collection.getId())
                .document(collection)
            ));
            return collection;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create collection", e);
        }
    }

    public Optional<CollectionEntity> getById(String id) {
        try {
            String indexName = indicesManager.createIfNotExist(null, CollectionEntity.class);
            GetResponse<CollectionEntity> resp = openSearchClient.get(GetRequest.of(g -> g.index(indexName).id(id)), CollectionEntity.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get collection by id", e);
        }
    }

    public List<CollectionEntity> list(int page, int size) {
        try {
            // For listing all collections, use a generic index name ("collections")
            String indexName = indicesManager.createIfNotExist(null, CollectionEntity.class);
            SearchResponse<CollectionEntity> resp = openSearchClient.search(SearchRequest.of(s -> s
                .index(indexName)
                .from(page * size)
                .size(size)
                .query(q -> q.matchAll(m -> m))
            ), CollectionEntity.class);
            List<CollectionEntity> results = new ArrayList<>();
            for (var hit : resp.hits().hits()) {
                results.add(hit.source());
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list collections", e);
        }
    }

    public CollectionEntity update(String id, CollectionEntity collection) {
        collection.setId(id);
        collection.setModified(java.time.Instant.now().toString());
        try {
            String indexName = indicesManager.createIfNotExist(null, CollectionEntity.class);
            openSearchClient.index(IndexRequest.of(i -> i
                .index(indexName)
                .id(id)
                .document(collection)
            ));
            return collection;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update collection", e);
        }
    }

    public boolean deleteById(String id) {
        try {
            // Get all documents in the collection (use a large size to get all, or implement paging if needed)
            List<DocumentEntity> documents = documentService.listDocuments(id, 0, 10000);
            for (DocumentEntity doc : documents) {
                documentService.deleteDocument(id, doc.getId());
            }
            String indexName = indicesManager.createIfNotExist(null, CollectionEntity.class);
            openSearchClient.delete(DeleteRequest.of(d -> d.index(indexName).id(id)));
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete collection by id", e);
        }
    }
}

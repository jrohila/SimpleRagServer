/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.domain.ChunkEntity;
import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.github.jrohila.simpleragserver.startup.OpenSearchSetup;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author Jukka
 */
@Service
public class IndicesManager {

    private static final Logger LOGGER = Logger.getLogger(IndicesManager.class.getName());

    private final OpenSearchClient client;

    @Value("${chunks.dimension-size}")
    private int embeddingDim;

    @Value("${chunks.similarity-function}")
    private String similarity; // cosinesimil | l2 | innerproduct

    private final Set<String> existingIndices = new HashSet<>();

    public IndicesManager(OpenSearchClient client) {
        this.client = client;
    }

    public String createIfNotExist(String collectionId, Class<?> type) throws Exception {
        String indexName = this.getIndexName(collectionId, type);
        if (!existingIndices.contains(indexName)) {
            if (DocumentEntity.class.equals(type)) {
                this.createDocumentsIndex(collectionId);
            } else {
                if (ChunkEntity.class.equals(type)) {
                    this.createChunksIndex(collectionId);
                }
            }
        }
        return indexName;
    }

    /**
     * Creates the documents index using the mapping from DocumentEntity.
     */
    private void createDocumentsIndex(String collectionId) throws Exception {
        String indexName = this.getIndexName(collectionId, DocumentEntity.class);

        BooleanResponse exists = client.indices().exists(b -> b.index(indexName));
        if (exists.value()) {
            LOGGER.log(Level.INFO, "OpenSearchSetup: index already exists: {0}", indexName);
            return;
        }

        CreateIndexRequest req = new CreateIndexRequest.Builder()
                .index(indexName)
                .settings(s -> s.index(i -> i
                .numberOfShards(1)
                .numberOfReplicas(0)
        ))
                .mappings(m -> m
                .properties("id", p -> p.keyword(k -> k))
                .properties("state", p -> p.keyword(k -> k))
                .properties("originalFilename", p -> p.text(t -> t))
                .properties("contentId", p -> p.keyword(k -> k))
                .properties("contentLen", p -> p.long_(l -> l))
                .properties("mimeType", p -> p.keyword(k -> k))
                .properties("hash", p -> p.keyword(k -> k))
                .properties("createdTime", p -> p.date(d -> d))
                .properties("updatedTime", p -> p.date(d -> d))
                )
                .build();

        client.indices().create(req);

        this.existingIndices.add(indexName);

        LOGGER.log(Level.INFO, "OpenSearchSetup: created index {0}", indexName);
    }

    private String getIndexName(String collectionId, Class<?> type) {
        String indexName;
        if (collectionId != null) {
            indexName = collectionId + "." + type.getTypeName();
        } else {
            indexName = type.getTypeName();
        }
        indexName = indexName.toLowerCase();
        
        return indexName;
    }

    private void createChunksIndex(String collectionId) throws Exception {
        String indexName = this.getIndexName(collectionId, ChunkEntity.class);

        BooleanResponse exists = client.indices().exists(b -> b.index(indexName));
        if (exists.value()) {
            LOGGER.log(Level.INFO, "OpenSearchSetup: index already exists: {0}", indexName);
            return;
        }

        CreateIndexRequest req = new CreateIndexRequest.Builder()
                .index(indexName)
                .settings(s -> s.index(i -> i
                .numberOfShards(1)
                .numberOfReplicas(0)
                .knn(true)
        ))
                .mappings(m -> m
                .properties("text", p -> p.text(t -> t
                .fields("keyword", f -> f.keyword(k -> k))
        ))
                .properties("type", p -> p.keyword(k -> k))
                .properties("sectionTitle", p -> p.text(t -> t))
                .properties("pageNumber", p -> p.integer(n -> n))
                .properties("language", p -> p.keyword(k -> k))
                .properties("hash", p -> p.keyword(k -> k))
                .properties("documentName", p -> p.text(t -> t))
                .properties("created", p -> p.date(d -> d))
                .properties("modified", p -> p.date(d -> d))
                .properties("documentId", p -> p.keyword(k -> k))
                .properties("embedding", p -> p.knnVector(k -> k
                .dimension(embeddingDim)
                .method(me -> me
                .name("hnsw")
                .engine("lucene")
                .spaceType(similarity)
                )
        ))
                )
                .build();

        client.indices().create(req);

        this.existingIndices.add(indexName);

        LOGGER.log(Level.INFO, "OpenSearchSetup: created index {0} (dimension={1}, space_type={2})", new Object[]{indexName, embeddingDim, similarity});
    }

}

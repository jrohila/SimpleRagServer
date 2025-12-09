package io.github.jrohila.simpleragserver.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.jrohila.simpleragserver.domain.HfModelEntity;
import io.github.jrohila.simpleragserver.client.hf.HfModelInfo;
import io.github.jrohila.simpleragserver.client.hf.HuggingFaceClient;
import io.github.jrohila.simpleragserver.repository.HfModelConverter;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class HfModelService {

    private static final Logger log = LoggerFactory.getLogger(HfModelService.class);

    @Autowired
    private IndicesManager indicesManager;

    @Autowired
    private OpenSearchClient openSearchClient;

    @Autowired
    private HuggingFaceClient hfClient;

    public HfModelEntity create(HfModelEntity model) {
        if (model.getId() == null || model.getId().isBlank()) {
            model.setId(java.util.UUID.randomUUID().toString());
        }
        try {
            log.info("Creating HF model entity: {}", model.getId());
            String indexName = indicesManager.createIfNotExist(null, HfModelEntity.class);
            openSearchClient.index(IndexRequest.of(i -> i
                    .index(indexName)
                    .id(model.getId())
                    .document(model)
            ));
            return model;
        } catch (Exception e) {
            log.error("Failed to create HF model entity: {}", model, e);
            throw new RuntimeException("Failed to create HF model entity", e);
        }
    }

    /**
     * Fetch all models from Hugging Face (page by page) and persist them into OpenSearch.
     * This method is synchronous and returns number of saved models.
     */
    public int fetchAndSaveAllModels() {
        int page = 0;
        int totalSaved = 0;
        while (true) {
            try {
                log.info("Fetching HF models page {}", page);
                var params = java.util.Map.of(
                        "author", "onnx-community",
                        "pipeline_tag", "text-generation",
                        "p", String.valueOf(page),
                        "withCount", "true",
                        "sort", "downloads"
                );
                List<HfModelInfo> infos = hfClient.getModelsFromHub(params);
                if (infos == null || infos.isEmpty()) {
                    break;
                }
                for (HfModelInfo info : infos) {
                    HfModelEntity ent = HfModelConverter.convert(info);
                    // use model id as document id
                    if (ent.getId() == null || ent.getId().isBlank()) ent.setId(info.getId());
                    update(ent.getId(), ent);
                    totalSaved++;
                }
                page++;
            } catch (Exception e) {
                log.error("Failed fetching or saving HF models on page {}", page, e);
                break;
            }
        }
        log.info("Completed HF model import, total saved={}", totalSaved);
        return totalSaved;
    }

    /**
     * Async wrapper for {@link #fetchAndSaveAllModels()}.
     */
    public java.util.concurrent.CompletableFuture<Integer> fetchAndSaveAllModelsAsync() {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> fetchAndSaveAllModels());
    }

    public Optional<HfModelEntity> getById(String id) {
        try {
            log.info("Getting HF model by id: {}", id);
            String indexName = indicesManager.createIfNotExist(null, HfModelEntity.class);
            GetResponse<HfModelEntity> resp = openSearchClient.get(GetRequest.of(g -> g.index(indexName).id(id)), HfModelEntity.class);
            if (resp.found()) {
                return Optional.ofNullable(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to get HF model by id: {}", id, e);
            throw new RuntimeException("Failed to get HF model by id", e);
        }
    }

    public List<HfModelEntity> list(int page, int size) {
        try {
            log.info("Listing HF models page={} size={}", page, size);
            String indexName = indicesManager.createIfNotExist(null, HfModelEntity.class);
            SearchResponse<HfModelEntity> resp = openSearchClient.search(SearchRequest.of(s -> s
                    .index(indexName)
                    .from(page * size)
                    .size(size)
                    .query(q -> q.matchAll(m -> m))
            ), HfModelEntity.class);
            List<HfModelEntity> results = new ArrayList<>();
            for (var hit : resp.hits().hits()) {
                HfModelEntity e = hit.source();
                results.add(e);
            }
            return results;
        } catch (Exception e) {
            log.error("Failed to list HF models page={} size={}", page, size, e);
            throw new RuntimeException("Failed to list HF models", e);
        }
    }

    public HfModelEntity update(String id, HfModelEntity model) {
        model.setId(id);
        try {
            log.info("Updating HF model id={}", id);
            String indexName = indicesManager.createIfNotExist(null, HfModelEntity.class);
            openSearchClient.index(IndexRequest.of(i -> i
                    .index(indexName)
                    .id(id)
                    .document(model)
            ));
            return model;
        } catch (Exception e) {
            log.error("Failed to update HF model id={}", id, e);
            throw new RuntimeException("Failed to update HF model", e);
        }
    }

    public boolean deleteById(String id) {
        try {
            log.info("Deleting HF model by id: {}", id);
            String indexName = indicesManager.createIfNotExist(null, HfModelEntity.class);
            openSearchClient.delete(DeleteRequest.of(d -> d.index(indexName).id(id)));
            return true;
        } catch (Exception e) {
            log.error("Failed to delete HF model by id: {}", id, e);
            throw new RuntimeException("Failed to delete HF model by id", e);
        }
    }

}

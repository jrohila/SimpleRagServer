package io.github.jrohila.simpleragserver.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.jrohila.simpleragserver.domain.ChatEntity;
import io.github.jrohila.simpleragserver.domain.LLMConfig;
import io.github.jrohila.simpleragserver.factory.LLMConfigFactory;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;

@Service
public class ChatManagerService {

    private static final Logger log = LoggerFactory.getLogger(ChatManagerService.class);

    @Autowired
    private IndicesManager indicesManager;

    @Autowired
    private OpenSearchClient openSearchClient;

    @Autowired
    private LLMConfigFactory llmConfigFactory;

    public ChatEntity create(ChatEntity chat) {
        if (chat.getId() == null || chat.getId().isBlank()) {
            chat.setId(java.util.UUID.randomUUID().toString());
        }

        // Initialize LLMConfig to RAG_QA default if not set
        if (chat.getLlmConfig() == null) {
            chat.setLlmConfig(llmConfigFactory.create(LLMConfig.UseCase.RAG_QA));
        }

        try {
            log.info("Creating chat: {}", chat);
            String indexName = indicesManager.createIfNotExist(null, ChatEntity.class);
            openSearchClient.index(IndexRequest.of(i -> i
                    .index(indexName)
                    .id(chat.getId())
                    .document(chat)
            ));
            log.info("Chat created with id {} in index {}", chat.getId(), indexName);
            return chat;
        } catch (Exception e) {
            log.error("Failed to create chat: {}", chat, e);
            throw new RuntimeException("Failed to create chat", e);
        }
    }

    public Optional<ChatEntity> getById(String id) {
        try {
            log.info("Getting chat by id: {}", id);
            String indexName = indicesManager.createIfNotExist(null, ChatEntity.class);
            GetResponse<ChatEntity> resp = openSearchClient.get(GetRequest.of(g -> g.index(indexName).id(id)), ChatEntity.class);
            if (resp.found()) {
                log.info("Chat found for id: {}", id);
                ChatEntity chat = resp.source();
                // Initialize LLMConfig to RAG_QA default if not set
                if (chat.getLlmConfig() == null) {
                    chat.setLlmConfig(llmConfigFactory.create(LLMConfig.UseCase.RAG_QA));
                }
                return Optional.of(chat);
            } else {
                log.info("No chat found for id: {}", id);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to get chat by id: {}", id, e);
            throw new RuntimeException("Failed to get chat by id", e);
        }
    }

    public List<ChatEntity> list(int page, int size) {
        try {
            log.info("Listing chats page={} size={}", page, size);
            String indexName = indicesManager.createIfNotExist(null, ChatEntity.class);
            SearchResponse<ChatEntity> resp = openSearchClient.search(SearchRequest.of(s -> s
                    .index(indexName)
                    .from(page * size)
                    .size(size)
                    .query(q -> q.matchAll(m -> m))
            ), ChatEntity.class);
            List<ChatEntity> results = new ArrayList<>();
            for (var hit : resp.hits().hits()) {
                ChatEntity chat = hit.source();
                // Initialize LLMConfig to RAG_QA default if not set
                if (chat.getLlmConfig() == null) {
                    chat.setLlmConfig(llmConfigFactory.create(LLMConfig.UseCase.RAG_QA));
                }
                results.add(chat);
            }
            log.info("Listed {} chats", results.size());
            return results;
        } catch (Exception e) {
            log.error("Failed to list chats page={} size={}", page, size, e);
            throw new RuntimeException("Failed to list chats", e);
        }
    }

    public ChatEntity update(String id, ChatEntity chat) {
        chat.setId(id);
        // Initialize LLMConfig to RAG_QA default if not set
        if (chat.getLlmConfig() == null) {
            chat.setLlmConfig(llmConfigFactory.create(LLMConfig.UseCase.RAG_QA));
        }
        try {
            log.info("Updating chat id={}: {}", id, chat);
            String indexName = indicesManager.createIfNotExist(null, ChatEntity.class);
            openSearchClient.index(IndexRequest.of(i -> i
                    .index(indexName)
                    .id(id)
                    .document(chat)
            ));
            log.info("Chat updated id={} in index {}", id, indexName);
            return chat;
        } catch (Exception e) {
            log.error("Failed to update chat id={}: {}", id, chat, e);
            throw new RuntimeException("Failed to update chat", e);
        }
    }

    public boolean deleteById(String id) {
        try {
            log.info("Deleting chat by id: {}", id);
            String indexName = indicesManager.createIfNotExist(null, ChatEntity.class);
            openSearchClient.delete(DeleteRequest.of(d -> d.index(indexName).id(id)));
            log.info("Deleted chat by id: {} from index {}", id, indexName);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete chat by id: {}", id, e);
            throw new RuntimeException("Failed to delete chat by id", e);
        }
    }

    public Optional<ChatEntity> getByPublicName(String publicName) {
        try {
            log.info("Getting chat by publicName: {}", publicName);
            String indexName = indicesManager.createIfNotExist(null, ChatEntity.class);

            // 1. Build the query object (using a match query)
            Query query = Query.of(q -> q
                    .match(m -> m
                    .field("publicName")
                    .query(FieldValue.of(publicName))
                    )
            );

            SearchResponse<ChatEntity> resp = openSearchClient.search(SearchRequest.of(s -> s
                    .index(indexName)
                    .size(1)
                    .query(query)
            ), ChatEntity.class);
            if (!resp.hits().hits().isEmpty()) {
                log.info("Chat found for publicName: {}", publicName);
                ChatEntity chat = resp.hits().hits().get(0).source();
                // Initialize LLMConfig to RAG_QA default if not set
                if (chat != null && chat.getLlmConfig() == null) {
                    chat.setLlmConfig(llmConfigFactory.create(LLMConfig.UseCase.RAG_QA));
                }
                return Optional.ofNullable(chat);
            } else {
                log.info("No chat found for publicName: {}", publicName);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to get chat by publicName: {}", publicName, e);
            throw new RuntimeException("Failed to get chat by publicName", e);
        }
    }

}

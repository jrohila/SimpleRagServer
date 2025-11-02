package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.domain.ChatEntity;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatManagerService {

    @Autowired
    private IndicesManager indicesManager;

    @Autowired
    private OpenSearchClient openSearchClient;

    public ChatEntity create(ChatEntity chat) {
        if (chat.getId() == null || chat.getId().isBlank()) {
            chat.setId(java.util.UUID.randomUUID().toString());
        }

        try {
            String indexName = indicesManager.createIfNotExist(null, ChatEntity.class);
            openSearchClient.index(IndexRequest.of(i -> i
                    .index(indexName)
                    .id(chat.getId())
                    .document(chat)
            ));
            return chat;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create chat", e);
        }
    }

    public Optional<ChatEntity> getById(String id) {
        try {
            String indexName = indicesManager.createIfNotExist(null, ChatEntity.class);
            GetResponse<ChatEntity> resp = openSearchClient.get(GetRequest.of(g -> g.index(indexName).id(id)), ChatEntity.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get chat by id", e);
        }
    }

    public List<ChatEntity> list(int page, int size) {
        try {
            String indexName = indicesManager.createIfNotExist(null, ChatEntity.class);
            SearchResponse<ChatEntity> resp = openSearchClient.search(SearchRequest.of(s -> s
                    .index(indexName)
                    .from(page * size)
                    .size(size)
                    .query(q -> q.matchAll(m -> m))
            ), ChatEntity.class);
            List<ChatEntity> results = new ArrayList<>();
            for (var hit : resp.hits().hits()) {
                results.add(hit.source());
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list chats", e);
        }
    }

    public ChatEntity update(String id, ChatEntity chat) {
        chat.setId(id);
        try {
            String indexName = indicesManager.createIfNotExist(null, ChatEntity.class);
            openSearchClient.index(IndexRequest.of(i -> i
                    .index(indexName)
                    .id(id)
                    .document(chat)
            ));
            return chat;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update chat", e);
        }
    }

    public boolean deleteById(String id) {
        try {
            String indexName = indicesManager.createIfNotExist(null, ChatEntity.class);
            openSearchClient.delete(DeleteRequest.of(d -> d.index(indexName).id(id)));
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete chat by id", e);
        }
    }
}

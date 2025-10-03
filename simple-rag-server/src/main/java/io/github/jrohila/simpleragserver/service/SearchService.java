package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.client.EmbedClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import io.github.jrohila.simpleragserver.dto.SearchResultDTO;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;

@Service
public class SearchService {

    private final RestTemplate restTemplate;
    private final EmbedClient embedService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public enum MatchType {
        MATCH, MATCH_PHRASE, MATCH_BOOL_PREFIX, QUERY_STRING, SIMPLE_QUERY_STRING
    };

    @Value("${opensearch.uris}")
    private String uris;

    @Value("${opensearch.username}")
    private String username;

    @Value("${opensearch.password}")
    private String password;

    @Value("${chunks.index-name}")
    private String indexName;

    public SearchService(EmbedClient embedService) {
        this.restTemplate = new RestTemplate();
        this.embedService = embedService;
    }

    public Map<String, Object> searchAsRaw(
            String textQuery,
            MatchType matchType,
            boolean useKnn,
            int size
    ) throws Exception {
        // Get embedding vector from EmbedService
        List<Float> embedding = embedService.getEmbeddingAsList(textQuery);

        List<Map<String, Object>> queries = new ArrayList<>();
        switch (matchType) {
            case MatchType.MATCH ->
                queries.add(Map.of("match", Map.of("text", textQuery)));
            case MatchType.MATCH_PHRASE ->
                queries.add(Map.of("match_phrase", Map.of("text", textQuery)));
            case MatchType.MATCH_BOOL_PREFIX ->
                queries.add(Map.of("match_bool_prefix", Map.of("text", textQuery)));
            case MatchType.QUERY_STRING ->
                queries.add(Map.of("query_string", Map.of("query", textQuery)));
            case MatchType.SIMPLE_QUERY_STRING ->
                queries.add(Map.of("simple_query_string", Map.of("query", textQuery)));
        }
        if (useKnn) {
            queries.add(Map.of("knn", Map.of("embedding", Map.of("vector", embedding, "k", 50))));
        }

        // Build hybrid query
        Map<String, Object> hybridQuery = Map.of(
                "hybrid", Map.of(
                        "queries", queries
                )
        );

        Map<String, Object> body = Map.of(
                "size", size,
                "query", hybridQuery
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Get OpenSearch connection info from environment
        if (username != null && password != null) {
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);
        }

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        String base = uris.split(",")[0].trim();
        //String searchEndpoint = String.format("%s/%s/_search", base.endsWith("/") ? base.substring(0, base.length() - 1) : base, indexName);
        String searchEndpoint = String.format("%s/%s/_search?search_pipeline=rrf-pipeline", base.endsWith("/") ? base.substring(0, base.length() - 1) : base, indexName);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                searchEndpoint,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
        }
        );

        return response.getBody();
    }

    public List<SearchResultDTO> search(String textQuery,
            MatchType matchType,
            boolean useKnn,
            int size) throws Exception {
        Map<String, Object> body = searchAsRaw(textQuery, matchType, useKnn, size);
        return SearchResponseMapper.toResults(body);
    }

}

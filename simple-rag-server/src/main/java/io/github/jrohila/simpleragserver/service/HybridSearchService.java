package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.client.EmbedClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

@Service
public class HybridSearchService {

    private final RestTemplate restTemplate;
    private final EmbedClient embedService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private Environment env;

    @Value("${opensearch.uris}")
    private String uris;

    @Value("${opensearch.username}")
    private String username;

    @Value("${opensearch.password}")
    private String password;

    @Value("${chunks.index-name}")
    private String indexName;

    public HybridSearchService(EmbedClient embedService) {
        this.restTemplate = new RestTemplate();
        this.embedService = embedService;
    }

    public Map<String, Object> hybridSearch(String textQuery, String type, String index) throws Exception {
        // Get embedding vector from EmbedService
        List<Float> embedding = embedService.getEmbeddingAsList(textQuery);

        // Build hybrid query
        Map<String, Object> hybridQuery = Map.of(
                "hybrid", Map.of(
                        "queries", List.of(
                                Map.of("match", Map.of("text", textQuery)),
                                Map.of("knn", Map.of("embedding", Map.of("vector", embedding, "k", 50)))
                        )
                )
        );

        Map<String, Object> body = Map.of("query", hybridQuery);

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
        String searchEndpoint = String.format("%s/%s/_search", base.endsWith("/") ? base.substring(0, base.length() - 1) : base, index);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                searchEndpoint,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
        }
        );

        return response.getBody();
    }

    private static String coalesce(String a, String b, String fallback) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return fallback;
    }
}

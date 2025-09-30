package io.github.jrohila.simpleragserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class HybridSearchService {

    private final RestTemplate restTemplate;
    private final EmbedService embedService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String openSearchUrl = "http://localhost:9200";

    @Autowired
    private Environment env;

    public HybridSearchService(EmbedService embedService) {
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
                    Map.of("match", Map.of("content", textQuery)),
                    Map.of("knn", Map.of("embedding", Map.of("vector", embedding, "k", 50)))
                )           
            )
        );

        Map<String, Object> body = Map.of("query", hybridQuery);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Get username and password from environment
        String username = env.getProperty("spring.ai.vectorstore.opensearch.username");
        String password = env.getProperty("spring.ai.vectorstore.opensearch.password");

        if (username != null && password != null) {
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);
        }

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        String searchEndpoint = String.format("%s/%s/_search", openSearchUrl, index);

        ResponseEntity<Map> response = restTemplate.exchange(
            searchEndpoint,
            HttpMethod.POST,
            entity,
            Map.class
        );

        return response.getBody();
    }
}
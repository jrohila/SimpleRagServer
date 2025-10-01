package io.github.jrohila.simpleragserver.startup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;

@Component
public class ChunksIndexCreator implements ApplicationRunner {

    private static final Logger LOGGER = Logger.getLogger(ChunksIndexCreator.class.getName());

    private final Environment env;

    @Autowired
    public ChunksIndexCreator(Environment env) {
        this.env = env;
    }

    @Value("${opensearch.uris}")
    private String uris;

    @Value("${opensearch.username}")
    private String username;

    @Value("${opensearch.password}")
    private String password;

    @Value("${chunks.index-name}")
    private String indexName;

    @Value("${chunks.dimension-size}")
    private Integer dimensionSize;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String firstUri = uris.split(",")[0].trim();
        URI indexUri = URI.create(firstUri.endsWith("/") ? firstUri + indexName : firstUri + "/" + indexName);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (username != null && !username.isBlank()) {
            headers.setBasicAuth(username, password == null ? "" : password);
        }

        // Check if index exists using HEAD
        try {
            restTemplate.exchange(indexUri, HttpMethod.HEAD, new HttpEntity<>(headers), Void.class);
            LOGGER.log(Level.INFO, "OpenSearch index already exists: {0}", indexName);
            return;
        } catch (HttpClientErrorException.NotFound nf) {
            // Not found; proceed to create
        } catch (HttpClientErrorException e) {
            // If unauthorized or other client error, log but attempt creation
            LOGGER.log(Level.INFO, "Index exists check returned status: {0}", e.getStatusCode());
        }

        Map<String, Object> settings = new HashMap<>();
        settings.put("index.knn", true);
        settings.put("number_of_shards", 1);
        settings.put("number_of_replicas", 0);

        Map<String, Object> properties = new HashMap<>();
        properties.put("text", Map.of("type", "text"));
        properties.put("type", Map.of("type", "keyword"));
        properties.put("sectionTitle", Map.of("type", "text"));
        properties.put("pageNumber", Map.of("type", "integer"));
        properties.put("language", Map.of("type", "keyword"));
        properties.put("created", Map.of("type", "date"));
        properties.put("modified", Map.of("type", "date"));
        properties.put("documentId", Map.of("type", "keyword"));

        Map<String, Object> method = Map.of(
                "name", "hnsw",
                "space_type", "cosinesimil",
                "engine", "lucene"
        );
        Map<String, Object> embedding = new HashMap<>();
        embedding.put("type", "knn_vector");
        embedding.put("dimension", dimensionSize);
        embedding.put("method", method);
        properties.put("embedding", embedding);

        Map<String, Object> mappings = Map.of("properties", properties);
        Map<String, Object> body = Map.of(
                "settings", settings,
                "mappings", mappings
        );

        HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> createResponse = restTemplate.exchange(indexUri, HttpMethod.PUT, createRequest, String.class);
            LOGGER.log(Level.INFO, "Created OpenSearch index: {0} (status={1}) with embedding dimension={2}", new Object[]{indexName, createResponse.getStatusCode(), dimensionSize});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create index: " + indexName, e);
            throw e;
        }
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

package io.github.jrohila.simpleragserver.startup;

import java.util.logging.Level;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jrohila.simpleragserver.domain.ChatEntity;
import io.github.jrohila.simpleragserver.domain.ChunkEntity;
import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.github.jrohila.simpleragserver.repository.IndicesManager;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class OpenSearchSetup implements ApplicationRunner {

    private static final Logger LOGGER = Logger.getLogger(OpenSearchSetup.class.getName());

    private final OpenSearchClient client;

    // OpenSearch connection (reused for pipeline HTTP call)
    @Value("${opensearch.uris}")
    private String osUri;
    @Value("${opensearch.username:}")
    private String username;
    @Value("${opensearch.password:}")
    private String password;

    @Autowired
    private IndicesManager indicesManager;

    public OpenSearchSetup(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Call individual creation methods here
        indicesManager.createIfNotExist(null, DocumentEntity.class);
        indicesManager.createIfNotExist(null, ChunkEntity.class);
        indicesManager.createIfNotExist(null, ChatEntity.class);

        createRffPipeline();
    }

    public void createRffPipeline() throws Exception {
        String pipelineId = "rrf-pipeline";

        Map<String, Object> body = Map.of(
                "description", "Post processor for hybrid RRF search",
                "phase_results_processors", List.of(
                        Map.of("score-ranker-processor",
                                Map.of("combination",
                                        Map.of("technique", "rrf",
                                                "rank_constant", 60,
                                                "window_size", 50)
                                )
                        )
                )
        );

        // Build request
        String base = osUri.endsWith("/") ? osUri.substring(0, osUri.length() - 1) : osUri;
        URI uri = URI.create(base + "/_search/pipeline/" + pipelineId);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(body);

        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json));

        if (username != null && !username.isBlank()) {
            String token = Base64.getEncoder().encodeToString((username + ":" + (password == null ? "" : password))
                    .getBytes(StandardCharsets.UTF_8));
            req.header("Authorization", "Basic " + token);
        }

        HttpClient http = HttpClient.newHttpClient();
        HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 == 2) {
            LOGGER.log(Level.INFO, "OpenSearchSetup: created/updated search pipeline {0}", pipelineId);
        } else {
            LOGGER.log(Level.WARNING, "OpenSearchSetup: pipeline PUT failed status={0} body={1}",
                    new Object[]{resp.statusCode(), resp.body()});
        }
    }
}

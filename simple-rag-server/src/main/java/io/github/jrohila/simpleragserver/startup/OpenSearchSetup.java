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
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class OpenSearchSetup implements ApplicationRunner {

    private static final Logger LOGGER = Logger.getLogger(OpenSearchSetup.class.getName());

    private final OpenSearchClient client;

    @Value("${documents.index-name}")
    private String documentIndexName;

    @Value("${chunks.index-name}")
    private String chunkIndexName;

    @Value("${chunks.dimension-size}")
    private int embeddingDim;

    @Value("${chunks.similarity-function}")
    private String similarity; // cosinesimil | l2 | innerproduct

    // OpenSearch connection (reused for pipeline HTTP call)
    @Value("${opensearch.uris}")
    private String osUri;
    @Value("${opensearch.username:}")
    private String username;
    @Value("${opensearch.password:}")
    private String password;

    public OpenSearchSetup(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Call individual creation methods here
        createDocumentsIndex();
        createChunksIndex();
        createRffPipeline();
    }

    /**
     * Creates the documents index using the mapping from DocumentEntity.
     */
    public void createDocumentsIndex() throws Exception {
        BooleanResponse exists = client.indices().exists(b -> b.index(documentIndexName));
        if (exists.value()) {
            LOGGER.log(Level.INFO, "OpenSearchSetup: index already exists: {0}", documentIndexName);
            return;
        }

        CreateIndexRequest req = new CreateIndexRequest.Builder()
                .index(documentIndexName)
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
        LOGGER.info("OpenSearchSetup: created index " + documentIndexName);
    }

    public void createChunksIndex() throws Exception {
        BooleanResponse exists = client.indices().exists(b -> b.index(chunkIndexName));
        if (exists.value()) {
            LOGGER.log(Level.INFO, "OpenSearchSetup: index already exists: {0}", chunkIndexName);
            return;
        }

        CreateIndexRequest req = new CreateIndexRequest.Builder()
                .index(chunkIndexName)
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
        LOGGER.info("OpenSearchSetup: created index " + chunkIndexName + " (dimension=" + embeddingDim + ", space_type=" + similarity + ")");
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
            LOGGER.info("OpenSearchSetup: created/updated search pipeline " + pipelineId);
        } else {
            LOGGER.log(Level.WARNING, "OpenSearchSetup: pipeline PUT failed status={0} body={1}",
                    new Object[]{resp.statusCode(), resp.body()});
        }
    }
}

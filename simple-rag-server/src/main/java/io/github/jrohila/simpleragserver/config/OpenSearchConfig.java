package io.github.jrohila.simpleragserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class OpenSearchConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenSearchConfig.class);

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    // Build the RestClient transport (closed on shutdown)
    @Bean(name = "openSearchTransport", destroyMethod = "close")
    public RestClientTransport openSearchTransport(
            @Value("${opensearch.uris}") String osUri,
            @Value("${opensearch.username}") String username,
            @Value("${opensearch.password}") String password
    ) {
        log.info("Configuring OpenSearch RestClient transport...");
        log.info("OpenSearch URI: {}", osUri);
        log.info("OpenSearch Username: {}", username);
        log.info("Password provided: {}", password != null && !password.isBlank());
        
        // Parse schema, host, and port from the URI
        java.net.URI uri = java.net.URI.create(osUri.trim());
        String schema = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();

        if (schema == null || host == null) {
            log.error("Invalid opensearch.uris value: {}", osUri);
            throw new IllegalArgumentException("Invalid opensearch.uris value: " + osUri);
        }
        if (port == -1) {
            port = "https".equalsIgnoreCase(schema) ? 443 : 9200;
        }
        
        log.info("Parsed OpenSearch connection - Schema: {}, Host: {}, Port: {}", schema, host, port);

        // Build RestClient
        RestClientBuilder restClientBuilder = RestClient.builder(
                new org.apache.hc.core5.http.HttpHost(schema, host, port));

        // Configure timeouts
        restClientBuilder.setRequestConfigCallback(rc -> {
            rc.setConnectTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(30));
            rc.setResponseTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(60));
            return rc;
        });

        // Configure HTTP client (auth caching and authentication)
        restClientBuilder.setHttpClientConfigCallback(hc -> {
            // Disable AuthCache in GraalVM native image to avoid reflection issues
            if (isGraalVmNativeImage()) {
                log.info("GraalVM native image detected - disabling HTTP auth caching");
                hc.disableAuthCaching();
            }
            return hc;
        });

        // Configure preemptive Basic authentication
        if (username != null && !username.isBlank()) {
            log.info("Configuring preemptive Basic authentication for OpenSearch RestClient");
            String auth = username + ":" + (password == null ? "" : password);
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + encodedAuth;
            
            restClientBuilder.setDefaultHeaders(new Header[]{
                new BasicHeader("Authorization", authHeader)
            });
            log.debug("RestClient configured with preemptive Authorization header");
        } else {
            log.warn("No username provided - OpenSearch authentication is disabled");
        }

        RestClient restClient = restClientBuilder.build();
        
        // Use Spring's ObjectMapper if available, otherwise create a default one
        ObjectMapper mapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
        log.info("OpenSearch RestClient transport configured successfully with Jackson mapper");
        
        return new RestClientTransport(restClient, new JacksonJsonpMapper(mapper));
    }

    // High-level OpenSearch client backed by the RestClient transport
    @Bean
    public OpenSearchClient openSearchClient(RestClientTransport openSearchTransport) {
        log.info("Creating OpenSearchClient bean");
        OpenSearchClient client = new OpenSearchClient(openSearchTransport);
        log.info("OpenSearchClient created successfully");
        return client;
    }

    /**
     * Checks if the application is running in a GraalVM native image.
     * @return true if running in native image, false otherwise
     */
    private boolean isGraalVmNativeImage() {
        return "runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"));
    }
}
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
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.env.Environment;
import jakarta.inject.Singleton;
import jakarta.annotation.PreDestroy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Factory
public class OpenSearchConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenSearchConfig.class);

    private final ObjectMapper objectMapper;
    private final Environment environment;

    public OpenSearchConfig(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    // Build the RestClient transport (closed on shutdown)
    @Singleton
    public RestClientTransport openSearchTransport(
            @Property(name = "opensearch.uris") String osUri,
            @Property(name = "opensearch.username") String username,
            @Property(name = "opensearch.password") String password
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
        
        // Use injected ObjectMapper
        log.info("OpenSearch RestClient transport configured successfully with Jackson mapper");
        
        return new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    }

    /**
     * Detect whether the application is running as a GraalVM native image.
     *
     * Priority:
     * 1) Check configuration property `graalvm.enabled` in application.yml
     * 2) Check environment variable `GRAALVM_NATIVE_IMAGE`
     * 3) Fall back to presence of GraalVM ImageInfo class on the classpath
     */
    private boolean isGraalVmNativeImage() {
        try {
            if (environment != null) {
                var prop = environment.getProperty("graalvm.enabled", Boolean.class);
                if (prop.isPresent()) {
                    return prop.get();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to read graalvm.enabled property from environment", e);
        }

        try {
            String env = System.getenv("GRAALVM_NATIVE_IMAGE");
            if (env != null && !env.isBlank()) {
                return Boolean.parseBoolean(env);
            }
        } catch (Exception e) {
            // ignore
        }

        try {
            Class.forName("org.graalvm.nativeimage.ImageInfo");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // High-level OpenSearch client backed by the RestClient transport
    @Singleton
    public OpenSearchClient openSearchClient(RestClientTransport openSearchTransport) {
        log.info("Creating OpenSearchClient bean");
        OpenSearchClient client = new OpenSearchClient(openSearchTransport);
        log.info("OpenSearchClient created successfully");
        return client;
    }
}
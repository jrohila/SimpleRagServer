package io.github.jrohila.simpleragserver.config;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.EnableElasticsearchAuditing;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

@Configuration
@EnableElasticsearchAuditing
public class OpenSearchConfig {

    @Bean(name = "elasticsearchMappingContext")
    SimpleElasticsearchMappingContext elasticsearchMappingContext() {
        return new SimpleElasticsearchMappingContext();
    }

    // Build the HC5 transport (closed on shutdown)
    @Bean(name = "openSearchTransport", destroyMethod = "close")
    public ApacheHttpClient5Transport openSearchTransport(
            @Value("${opensearch.uris}") String osUri,
            @Value("${opensearch.username}") String username,
            @Value("${opensearch.password}") String password
    ) {
        // Parse schema, host, and port from the URI
        java.net.URI uri = java.net.URI.create(osUri.trim());
        String schema = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();

        if (schema == null || host == null) {
            throw new IllegalArgumentException("Invalid opensearch.uris value: " + osUri);
        }
        if (port == -1) {
            port = "https".equalsIgnoreCase(schema) ? 443 : 9200;
        }

        ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(
                        new org.apache.hc.core5.http.HttpHost(schema, host, port))
                .setMapper(new JacksonJsonpMapper())
                .setRequestConfigCallback(rc -> {
                    rc.setConnectTimeout(Timeout.ofSeconds(30));
                    rc.setResponseTimeout(Timeout.ofSeconds(60));
                    return rc;
                });

        if (username != null && !username.isBlank()) {
            BasicCredentialsProvider creds = new BasicCredentialsProvider();
            creds.setCredentials(new AuthScope(null, -1),
                    new UsernamePasswordCredentials(username, password == null ? new char[0] : password.toCharArray()));
            builder.setHttpClientConfigCallback(hc -> {
                hc.setDefaultCredentialsProvider(creds);
                return hc;
            });
        }

        return builder.build();
    }

    // High-level OpenSearch client backed by the HC5 transport
    @Bean
    public OpenSearchClient openSearchClient(ApacheHttpClient5Transport openSearchTransport) {
        return new OpenSearchClient(openSearchTransport);
    }
}

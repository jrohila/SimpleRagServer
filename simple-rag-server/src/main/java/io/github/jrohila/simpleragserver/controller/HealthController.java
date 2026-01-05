package io.github.jrohila.simpleragserver.controller;

import org.opensearch.client.opensearch.OpenSearchClient;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Controller("/api/health")
public class HealthController {

    private final OpenSearchClient openSearchClient;
    private final String ollamaBaseUrl;
    private final String doclingBaseUrl;

    public HealthController(
            OpenSearchClient openSearchClient,
            @Property(name = "llm.ollama.baseUrl", defaultValue = "") String ollamaBaseUrl,
            @Property(name = "docling-serve.url", defaultValue = "") String doclingBaseUrl) {
        this.openSearchClient = openSearchClient;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.doclingBaseUrl = doclingBaseUrl;
    }

    @Get("/database")
    public HttpResponse<Map<String, Object>> database() {
        Map<String, Object> m = new HashMap<>();
        try {
            var resp = openSearchClient.cluster().health();
            m.put("status", "ok");
            m.put("cluster_status", resp.status() != null ? resp.status().jsonValue() : "unknown");
            m.put("timestamp", Instant.now().toString());
            return HttpResponse.ok(m);
        } catch (Exception e) {
            m.put("status", "unhealthy");
            m.put("error", e.getMessage());
            m.put("timestamp", Instant.now().toString());
            return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        }
    }

    @Get("/llm")
    public HttpResponse<Map<String, Object>> llm() {
        Map<String, Object> m = new HashMap<>();
        if (ollamaBaseUrl == null || ollamaBaseUrl.isBlank()) {
            m.put("status", "not-configured");
            return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        }
        try {
            boolean ok = pingUrl(ollamaBaseUrl + "/api/tags", 2000);
            m.put("status", ok ? "ok" : "unreachable");
            m.put("timestamp", Instant.now().toString());
            return ok ? HttpResponse.ok(m) : HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        } catch (SocketTimeoutException e) {
            m.put("status", "timeout");
            return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        } catch (Exception e) {
            m.put("status", "error");
            m.put("error", e.getMessage());
            return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        }
    }

    @Get("/docling")
    public HttpResponse<Map<String, Object>> docling() {
        Map<String, Object> m = new HashMap<>();
        if (doclingBaseUrl == null || doclingBaseUrl.isBlank()) {
            m.put("status", "not-configured");
            return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        }
        try {
            boolean ok = pingUrl(doclingBaseUrl + "/health", 2000);
            m.put("status", ok ? "ok" : "unreachable");
            m.put("timestamp", Instant.now().toString());
            return ok ? HttpResponse.ok(m) : HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        } catch (SocketTimeoutException e) {
            m.put("status", "timeout");
            return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        } catch (Exception e) {
            m.put("status", "error");
            m.put("error", e.getMessage());
            return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        }
    }

    private boolean pingUrl(String urlStr, int timeoutMs) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false);
        conn.connect();
        int code = conn.getResponseCode();
        try (InputStream is = conn.getInputStream()) {
            /* drain */ }
        return code >= 200 && code < 300;
    }

    @Get("/ping")
    public HttpResponse<Map<String, Object>> ping() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "UP");
        payload.put("timestamp", Instant.now().toString());
        return HttpResponse.ok(payload);
    }
}

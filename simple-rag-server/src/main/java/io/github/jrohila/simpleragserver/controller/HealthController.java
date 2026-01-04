package io.github.jrohila.simpleragserver.controller;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private OpenSearchClient openSearchClient;

    @Value("${llm.ollama.baseUrl:}")
    private String ollamaBaseUrl;

    @Value("${docling-serve.url:}")
    private String doclingBaseUrl;

    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> database() {
        Map<String, Object> m = new HashMap<>();
        try {
            var resp = openSearchClient.cluster().health();
            m.put("status", "ok");
            m.put("cluster_status", resp.status() != null ? resp.status().jsonValue() : "unknown");
            m.put("timestamp", Instant.now().toString());
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            m.put("status", "unhealthy");
            m.put("error", e.getMessage());
            m.put("timestamp", Instant.now().toString());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        }
    }

    @GetMapping("/llm")
    public ResponseEntity<Map<String, Object>> llm() {
        Map<String, Object> m = new HashMap<>();
        if (ollamaBaseUrl == null || ollamaBaseUrl.isBlank()) {
            m.put("status", "not-configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        }
        try {
            boolean ok = pingUrl(ollamaBaseUrl + "/api/tags", 2000);
            m.put("status", ok ? "ok" : "unreachable");
            m.put("timestamp", Instant.now().toString());
            return ok ? ResponseEntity.ok(m) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        } catch (SocketTimeoutException e) {
            m.put("status", "timeout");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        } catch (Exception e) {
            m.put("status", "error");
            m.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        }
    }

    @GetMapping("/docling")
    public ResponseEntity<Map<String, Object>> docling() {
        Map<String, Object> m = new HashMap<>();
        if (doclingBaseUrl == null || doclingBaseUrl.isBlank()) {
            m.put("status", "not-configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        }
        try {
            boolean ok = pingUrl(doclingBaseUrl + "/health", 2000);
            m.put("status", ok ? "ok" : "unreachable");
            m.put("timestamp", Instant.now().toString());
            return ok ? ResponseEntity.ok(m) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        } catch (SocketTimeoutException e) {
            m.put("status", "timeout");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        } catch (Exception e) {
            m.put("status", "error");
            m.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
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

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "UP");
        payload.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(payload);
    }
}

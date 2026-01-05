package io.github.jrohila.simpleragserver.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.jrohila.simpleragserver.domain.DoclingConversionRequest;
import io.github.jrohila.simpleragserver.domain.DoclingConversionResponse;
import io.github.jrohila.simpleragserver.domain.DoclingChunkRequest;
import io.github.jrohila.simpleragserver.domain.DoclingChunkResponse;
import io.github.jrohila.simpleragserver.domain.Options;
import io.github.jrohila.simpleragserver.domain.SourceInput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import jakarta.inject.Singleton;
import io.micronaut.http.multipart.CompletedFileUpload;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Async Docling client that starts conversion jobs and returns a polling handle.
 * This does NOT poll; it only starts the operation. The async path is configurable.
 */
@Singleton
public class DoclingAsyncClient {

    private static final Logger logger = LoggerFactory.getLogger(DoclingAsyncClient.class);

    private final HttpClient httpClient;
    private final String doclingBaseUrl;
    private final String asyncConvertPath;
    private final ObjectMapper objectMapper;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int resultReadTimeoutMs;
    private final String statusPathTemplate;

    public DoclingAsyncClient(
            @Property(name = "docling-serve.url") String doclingBaseUrl,
            @Property(name = "docling.async.convert-path", defaultValue = "/v1/convert/source/async") String asyncConvertPath,
            @Property(name = "docling.async.status-path-template", defaultValue = "/v1/status/poll/{id}") String statusPathTemplate,
            @Property(name = "docling.timeout.connect", defaultValue = "10000") int connectTimeoutMs,
            @Property(name = "docling.timeout.read", defaultValue = "600000") int readTimeoutMs,
            @Property(name = "docling.result.timeout.read", defaultValue = "15000") int resultReadTimeoutMs,
            HttpClient httpClient
    ) {
        this.doclingBaseUrl = doclingBaseUrl;
        this.asyncConvertPath = asyncConvertPath;
        this.statusPathTemplate = statusPathTemplate;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.resultReadTimeoutMs = resultReadTimeoutMs;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * Start async conversion from URL.
     */
    public StartOperationResult convertFromUrl(String url) {
        DoclingConversionRequest req = DoclingConversionRequest.fromUrl(url);
        return executeAsyncConversion(req);
    }

    /**
     * Start async conversion from uploaded file.
     */
    public StartOperationResult convertFromFile(CompletedFileUpload file) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        DoclingConversionRequest req = DoclingConversionRequest.fromBase64(file.getFilename(), base64);
        return executeAsyncConversion(req);
    }

    /**
     * Start async conversion from bytes.
     */
    public StartOperationResult convertFromBytes(String filename, byte[] content) {
        String base64 = Base64.getEncoder().encodeToString(content);
        DoclingConversionRequest req = DoclingConversionRequest.fromBase64(filename, base64);
        return executeAsyncConversion(req);
    }

    private StartOperationResult executeAsyncConversion(DoclingConversionRequest request) {
        try {
            String url = doclingBaseUrl + asyncConvertPath;
            try {
                Object redacted = buildRedactedRequestLog(request);
                String payload = objectMapper.writeValueAsString(redacted);
                logger.info("Docling ASYNC request POST {} payload_redacted={}", url, payload);
            } catch (Exception ignore) {
                logger.debug("Docling ASYNC request serialization for logging failed (payload redacted)");
            }

            HttpRequest<DoclingConversionRequest> httpReq = HttpRequest.POST(url, request)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);

            HttpResponse<String> response = httpClient.toBlocking().exchange(httpReq, String.class);

            int status = response.getStatus().getCode();
                String locHeader = response.getHeaders().get("location");
                URI loc = null;
                if (locHeader != null && !locHeader.isBlank()) {
                    try { loc = URI.create(locHeader); } catch (Exception ignore) {}
                }
            String body = response.getBody().orElse("");
            // Try to parse an operation id from body
            String opId = null;
            try {
                JsonNode root = objectMapper.readTree(body);
                if (root.hasNonNull("task_id")) opId = root.get("task_id").asText();
                else if (root.hasNonNull("id")) opId = root.get("id").asText();
                else if (root.hasNonNull("operation_id")) opId = root.get("operation_id").asText();
            } catch (Exception ignore) {}

            // Derive poll URL if Location not provided
            String pollUrl = (loc != null ? loc.toString() : null);
            if ((pollUrl == null || pollUrl.isBlank()) && opId != null && !opId.isBlank()) {
                pollUrl = doclingBaseUrl + statusPathTemplate.replace("{id}", opId);
            }

            logger.info("Docling ASYNC started: status={} location={} operationId={} bodyLen={}", status, pollUrl, opId, body.length());
            return new StartOperationResult(opId, pollUrl, status, body);
        } catch (io.micronaut.http.client.exceptions.HttpClientResponseException e) {
            String body = "";
            try { body = e.getResponse().getBody(String.class).orElse(""); } catch (Exception ex) {}
            logger.error("Docling ASYNC returned {} with body: {}", e.getStatus(), body);
            throw new RuntimeException("Docling async 4xx error: " + e.getStatus() + " body=" + body, e);
        } catch (Exception e) {
            logger.error("Failed to start async conversion using Docling service", e);
            throw new RuntimeException("Async conversion start failed: " + e.getMessage(), e);
        }
    }

    private Object buildRedactedRequestLog(DoclingConversionRequest request) {
        try {
            // Reuse DoclingClient's approach: only show lengths and filenames
            var helper = new java.util.HashMap<String, Object>();
            if (request.getOptions() != null) {
                helper.put("to_formats", request.getOptions().getToFormats());
                helper.put("from_formats", request.getOptions().getFromFormats());
            }
            var sources = new java.util.ArrayList<java.util.Map<String, Object>>();
            if (request.getSources() != null) {
                for (SourceInput s : request.getSources()) {
                    var m = new java.util.HashMap<String, Object>();
                    m.put("kind", s.getKind());
                    if (s.getUrl() != null) m.put("url", s.getUrl());
                    if (s.getFilename() != null) m.put("filename", s.getFilename());
                    if (s.getBase64String() != null) {
                        m.put("base64_length", s.getBase64String().length());
                        m.put("base64_string", "[redacted]");
                    }
                    sources.add(m);
                }
            }
            helper.put("sources", sources);
            return helper;
        } catch (Exception ex) {
            return java.util.Map.of("info", "redaction_failed");
        }
    }

    // ===================== Async CHUNK start helpers =====================

    /**
     * Start async Hybrid chunking from URL.
     */
    public StartOperationResult hybridChunkFromUrl(
            String url,
            DoclingChunkRequest.HybridChunkerOptions hybridOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            Options convertOptionsOverride
    ) {
        DoclingChunkRequest req = new DoclingChunkRequest();
        SourceInput src = new SourceInput();
        src.setKind("http");
        src.setUrl(url);
        req.setSources(List.of(src));
        if (convertOptionsOverride != null) req.setConvertOptions(convertOptionsOverride);
        req.setChunkingOptions(hybridOptions);
        if (includeConvertedDoc != null) req.setIncludeConvertedDoc(includeConvertedDoc);
        if (targetKind != null && !targetKind.isBlank()) {
            DoclingChunkRequest.Target t = new DoclingChunkRequest.Target();
            t.setKind(targetKind);
            req.setTarget(t);
        }
        return executeAsyncStart("/v1/chunk/hybrid/source/async", req);
    }

    /**
     * Start async Hybrid chunking from file upload.
     */
    public StartOperationResult hybridChunkFromFile(
            CompletedFileUpload file,
            DoclingChunkRequest.HybridChunkerOptions hybridOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            Options convertOptionsOverride
    ) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        DoclingChunkRequest req = new DoclingChunkRequest();
        SourceInput src = new SourceInput();
        src.setKind("file");
        src.setFilename(file.getFilename());
        src.setBase64String(base64);
        req.setSources(List.of(src));
        if (convertOptionsOverride != null) req.setConvertOptions(convertOptionsOverride);
        req.setChunkingOptions(hybridOptions);
        if (includeConvertedDoc != null) req.setIncludeConvertedDoc(includeConvertedDoc);
        if (targetKind != null && !targetKind.isBlank()) {
            DoclingChunkRequest.Target t = new DoclingChunkRequest.Target();
            t.setKind(targetKind);
            req.setTarget(t);
        }
        return executeAsyncStart("/v1/chunk/hybrid/source/async", req);
    }

    /**
     * Start async Hybrid chunking from raw bytes.
     */
    public StartOperationResult hybridChunkFromBytes(
            String filename,
            byte[] content,
            DoclingChunkRequest.HybridChunkerOptions hybridOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            Options convertOptionsOverride
    ) {
        String base64 = Base64.getEncoder().encodeToString(content);
        DoclingChunkRequest req = new DoclingChunkRequest();
        SourceInput src = new SourceInput();
        src.setKind("file");
        src.setFilename(filename);
        src.setBase64String(base64);
        req.setSources(List.of(src));
        if (convertOptionsOverride != null) req.setConvertOptions(convertOptionsOverride);
        req.setChunkingOptions(hybridOptions);
        if (includeConvertedDoc != null) req.setIncludeConvertedDoc(includeConvertedDoc);
        if (targetKind != null && !targetKind.isBlank()) {
            DoclingChunkRequest.Target t = new DoclingChunkRequest.Target();
            t.setKind(targetKind);
            req.setTarget(t);
        }
        return executeAsyncStart("/v1/chunk/hybrid/source/async", req);
    }

    /**
     * Start async Hierarchical chunking from URL.
     */
    public StartOperationResult hierarchicalChunkFromUrl(
            String url,
            DoclingChunkRequest.HierarchicalChunkerOptions hierarchicalOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            Options convertOptionsOverride
    ) {
        DoclingChunkRequest req = new DoclingChunkRequest();
        SourceInput src = new SourceInput();
        src.setKind("http");
        src.setUrl(url);
        req.setSources(List.of(src));
        if (convertOptionsOverride != null) req.setConvertOptions(convertOptionsOverride);
        req.setChunkingOptions(hierarchicalOptions);
        if (includeConvertedDoc != null) req.setIncludeConvertedDoc(includeConvertedDoc);
        if (targetKind != null && !targetKind.isBlank()) {
            DoclingChunkRequest.Target t = new DoclingChunkRequest.Target();
            t.setKind(targetKind);
            req.setTarget(t);
        }
        return executeAsyncStart("/v1/chunk/hierarchical/source/async", req);
    }

    /**
     * Start async Hierarchical chunking from file upload.
     */
    public StartOperationResult hierarchicalChunkFromFile(
            CompletedFileUpload file,
            DoclingChunkRequest.HierarchicalChunkerOptions hierarchicalOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            Options convertOptionsOverride
    ) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        DoclingChunkRequest req = new DoclingChunkRequest();
        SourceInput src = new SourceInput();
        src.setKind("file");
        src.setFilename(file.getFilename());
        src.setBase64String(base64);
        req.setSources(List.of(src));
        if (convertOptionsOverride != null) req.setConvertOptions(convertOptionsOverride);
        req.setChunkingOptions(hierarchicalOptions);
        if (includeConvertedDoc != null) req.setIncludeConvertedDoc(includeConvertedDoc);
        if (targetKind != null && !targetKind.isBlank()) {
            DoclingChunkRequest.Target t = new DoclingChunkRequest.Target();
            t.setKind(targetKind);
            req.setTarget(t);
        }
        return executeAsyncStart("/v1/chunk/hierarchical/source/async", req);
    }

    /**
     * Generic helper to POST an async start request and return StartOperationResult.
     */
    private StartOperationResult executeAsyncStart(String path, Object payload) {
        try {
            String url = doclingBaseUrl + path;

            // Minimal redacted logging
            try {
                String json = objectMapper.writeValueAsString(payload);
                logger.info("Docling ASYNC start POST {} payload_size={}B", url, json.length());
            } catch (Exception ignore) {}

            HttpRequest<Object> httpReq = HttpRequest.POST(url, payload)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);

            HttpResponse<String> response = httpClient.toBlocking().exchange(httpReq, String.class);

            int status = response.getStatus().getCode();
                String locHeader = response.getHeaders().get("location");
                URI loc = null;
                if (locHeader != null && !locHeader.isBlank()) {
                    try { loc = URI.create(locHeader); } catch (Exception ignore) {}
                }
            String body = response.getBody().orElse("");

            String opId = null;
            try {
                JsonNode root = objectMapper.readTree(body);
                if (root.hasNonNull("task_id")) opId = root.get("task_id").asText();
                else if (root.hasNonNull("id")) opId = root.get("id").asText();
                else if (root.hasNonNull("operation_id")) opId = root.get("operation_id").asText();
            } catch (Exception ignore) {}

            String pollUrl = (loc != null ? loc.toString() : null);
            if ((pollUrl == null || pollUrl.isBlank()) && opId != null && !opId.isBlank()) {
                pollUrl = doclingBaseUrl + statusPathTemplate.replace("{id}", opId);
            }

            logger.info("Docling ASYNC chunk started: status={} location={} operationId={} bodyLen={}", status, pollUrl, opId, body.length());
            return new StartOperationResult(opId, pollUrl, status, body);
        } catch (io.micronaut.http.client.exceptions.HttpClientResponseException e) {
            String body = "";
            try { body = e.getResponse().getBody(String.class).orElse(""); } catch (Exception ex) {}
            logger.error("Docling ASYNC returned {} with body: {}", e.getStatus(), body);
            throw new RuntimeException("Docling async 4xx error: " + e.getStatus() + " body=" + body, e);
        } catch (Exception e) {
            logger.error("Failed to start async chunking using Docling service", e);
            throw new RuntimeException("Async chunk start failed: " + e.getMessage(), e);
        }
    }

    /**
     * Handle returned when starting an async operation.
     */
    public record StartOperationResult(String operationId, String pollUrl, int httpStatus, String rawBody) {}

    // ===================== Status and Result APIs =====================

    /**
     * Represents the current status of an async Docling operation.
     */
    public record OperationStatus(String id, String status, Integer progress, String resultUrl, String error, JsonNode raw) {
        public boolean isTerminal() {
            if (status == null) return false;
            String s = status.toLowerCase();
            return s.equals("success") || s.equals("succeeded") || s.equals("completed") || s.equals("done") ||
                   s.equals("failed") || s.equals("error") || s.equals("cancelled") || s.equals("canceled");
        }
        public boolean isSuccess() {
            if (status == null) return false;
            String s = status.toLowerCase();
            return s.equals("success") || s.equals("succeeded") || s.equals("completed") || s.equals("done");
        }
    }

    /**
     * GET operation status by full poll URL returned by Docling.
     */
    public OperationStatus getStatusByUrl(String pollUrl) {
        try {
            String u = pollUrl == null ? null : pollUrl.trim();
            if (u == null || u.isEmpty()) throw new IllegalArgumentException("pollUrl is empty");
            boolean looksLikeId = !u.contains("://") && !u.contains("/") && u.length() >= 8;
            if (looksLikeId) {
                // Treat as operation id
                return getStatusById(u); // Delegate to getStatusById if it looks like an ID
            }
            String absoluteUrl = toAbsoluteUrl(u);
            return getStatusByAbsoluteUrl(absoluteUrl);
        } catch (Exception e) {
            logger.error("Failed to fetch async status by URL", e);
            throw new RuntimeException("Fetching status failed: " + e.getMessage(), e);
        }
    }

    private OperationStatus getStatusByAbsoluteUrl(String absoluteUrl) {
        try {
            HttpRequest<?> req = HttpRequest.GET(absoluteUrl).accept(MediaType.APPLICATION_JSON);
            HttpResponse<String> resp = httpClient.toBlocking().exchange(req, String.class);
            int code = resp.getStatus().getCode();
            String body = resp.getBody().orElse("");
            if (logger.isDebugEnabled()) {
                logger.debug("Docling ASYNC status raw: code={} body={}", code, body.length() > 512 ? body.substring(0, 512) + "..." : body);
            }
            JsonNode root = objectMapper.readTree(body);
            OperationStatus st = parseOperationStatus(root);
            logger.info("Docling ASYNC status: url={} status={} progress={} id={}", absoluteUrl, st.status, st.progress, st.id);
            return st;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String toAbsoluteUrl(String url) {
        if (url == null || url.isBlank()) return url;
        String u = url.trim();
        if (u.startsWith("http://") || u.startsWith("https://")) return u;
        String base = this.doclingBaseUrl != null ? this.doclingBaseUrl.trim() : "";
        if (base.isEmpty()) return u; // best effort; will fail upstream but not null
        boolean baseEnds = base.endsWith("/");
        boolean urlStarts = u.startsWith("/");
        if (baseEnds && urlStarts) {
            return base.substring(0, base.length() - 1) + u;
        } else if (!baseEnds && !urlStarts) {
            return base + "/" + u;
        }
        return base + u;
    }

    /**
     * GET operation status by operation id using statusPathTemplate.
     */
    public OperationStatus getStatusById(String operationId) {
        String path = statusPathTemplate.replace("{id}", operationId);
        String url = doclingBaseUrl + path;
        return getStatusByAbsoluteUrl(url);
    }

    /**
     * Poll until a terminal status or timeout. intervalMs must be > 0.
     */
    public PollResult pollUntilTerminal(String pollUrlOrId, long timeoutMs, long intervalMs) {
        long start = System.currentTimeMillis();
        OperationStatus status;
        do {
            status = getStatusById(pollUrlOrId); // Always use getStatusById
            if (status.isTerminal()) break;
            try { Thread.sleep(Math.max(50L, intervalMs)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        } while (System.currentTimeMillis() - start < Math.max(0L, timeoutMs));
        DoclingConversionResponse response = null;
        if (status != null && status.isSuccess()) {
            response = fetchResult(status).orElse(null);
        }
        return new PollResult(status, response);
    }

    /**
     * Poll until terminal specifically for chunking flows and return DoclingChunkResponse.
     */
    public PollChunkResult pollChunkUntilTerminal(String pollUrlOrId, long timeoutMs, long intervalMs) {
        long start = System.currentTimeMillis();
        OperationStatus status;
        do {
            status = getStatusById(pollUrlOrId);
            if (status.isTerminal()) break;
            try { Thread.sleep(Math.max(50L, intervalMs)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        } while (System.currentTimeMillis() - start < Math.max(0L, timeoutMs));
        DoclingChunkResponse response = null;
        if (status != null && status.isSuccess()) {
            response = fetchChunkResult(status).orElse(null);
        }
        return new PollChunkResult(status, response);
    }

    /**
     * Try to extract a DoclingConversionResponse from the status JSON, or follow a result URL if present.
     */
    public Optional<DoclingConversionResponse> fetchResult(OperationStatus status) {
        try {
            // Attempt inline result first
            if (status.raw != null) {
                JsonNode resultNode = null;
                if (status.raw.has("result")) resultNode = status.raw.get("result");
                else if (status.raw.has("data")) resultNode = status.raw.get("data");
                if (resultNode != null && !resultNode.isNull()) {
                    DoclingConversionResponse r = objectMapper.treeToValue(resultNode, DoclingConversionResponse.class);
                    return Optional.ofNullable(r);
                }
            }
            // Else follow result URL
            if (status.resultUrl != null && !status.resultUrl.isBlank()) {
                HttpRequest<?> req = HttpRequest.GET(status.resultUrl).accept(MediaType.APPLICATION_JSON);
                int attempts = 3; // small retry to bridge status/result race
                for (int i = 0; i < attempts; i++) {
                    try {
                        HttpResponse<DoclingConversionResponse> resp = httpClient.toBlocking().exchange(req, DoclingConversionResponse.class);
                        if (resp.getStatus().getCode() >= 200 && resp.getStatus().getCode() < 300) {
                            return Optional.ofNullable(resp.getBody().orElse(null));
                        }
                        // If 404/425 etc., small wait and retry once or twice
                        Thread.sleep(250L);
                    } catch (Exception ex) {
                        // On timeout or other transient error, brief backoff then retry
                        try { Thread.sleep(250L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Failed to fetch async result: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Try to extract a DoclingChunkResponse from the status JSON, or follow a result URL if present.
     */
    public Optional<DoclingChunkResponse> fetchChunkResult(OperationStatus status) {
        try {
            // Attempt inline result first
            if (status.raw != null) {
                JsonNode resultNode = null;
                if (status.raw.has("result")) resultNode = status.raw.get("result");
                else if (status.raw.has("data")) resultNode = status.raw.get("data");
                if (resultNode != null && !resultNode.isNull()) {
                    logger.info("DoclingAsyncClient: Raw result JSON from inline status: {}", resultNode.toString());
                    DoclingChunkResponse r = objectMapper.treeToValue(resultNode, DoclingChunkResponse.class);
                    logger.info("DoclingAsyncClient: Deserialized DoclingChunkResponse: chunks={}", r != null ? (r.getChunks() != null ? r.getChunks().size() : "null") : "null response");
                    return Optional.ofNullable(r);
                }
            }
            // Else follow result URL
            if (status.resultUrl != null && !status.resultUrl.isBlank()) {
                HttpRequest<?> req = HttpRequest.GET(status.resultUrl).accept(MediaType.APPLICATION_JSON);

                int attempts = 3;
                for (int i = 0; i < attempts; i++) {
                    try {
                        logger.info("DoclingAsyncClient: Fetching chunk result from URL: {}", status.resultUrl);
                        HttpResponse<String> rawResp = httpClient.toBlocking().exchange(req, String.class);
                        if (rawResp.getStatus().getCode() >= 200 && rawResp.getStatus().getCode() < 300) {
                            String rawBody = rawResp.getBody().orElse("");
                            logger.debug("DoclingAsyncClient: Raw response body from result URL: {}", rawBody);
                            DoclingChunkResponse parsed = objectMapper.readValue(rawBody, DoclingChunkResponse.class);
                            logger.debug("DoclingAsyncClient: Deserialized DoclingChunkResponse from URL: chunks={}", parsed != null ? (parsed.getChunks() != null ? parsed.getChunks().size() : "null") : "null response");
                            return Optional.ofNullable(parsed);
                        }
                        Thread.sleep(250L);
                    } catch (Exception ex) {
                        logger.warn("DoclingAsyncClient: Attempt {} failed to fetch result from URL: {}", i + 1, ex.getMessage());
                        try { Thread.sleep(250L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                }
            }
            logger.warn("DoclingAsyncClient: No result found in status.raw or resultUrl");
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Failed to fetch async chunk result: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Final result of a polling run: terminal status plus optional conversion payload.
     */
    public record PollResult(OperationStatus status, DoclingConversionResponse response) {}

    /** Result of polling a chunking job */
    public record PollChunkResult(OperationStatus status, DoclingChunkResponse response) {}

    private OperationStatus parseOperationStatus(JsonNode root) {
        String id = firstNonNullText(root, "task_id", "id", "operation_id");
        String status = firstNonNullText(root, "task_status", "status", "state");
        // Progress: derive from task_meta if present
        Integer progress = null;
        JsonNode meta = root.path("task_meta");
        if (meta != null && meta.has("num_docs") && meta.has("num_processed")) {
            try {
                int nd = meta.path("num_docs").asInt(0);
                int np = meta.path("num_processed").asInt(0);
                if (nd > 0) progress = Math.max(0, Math.min(100, (int)Math.floor((np * 100.0) / nd)));
            } catch (Exception ignore) {}
        }
        // Docling doesn't return result url in status; derive from id
        String resultUrl = (id != null && !id.isBlank()) ? (doclingBaseUrl + "/v1/result/" + id) : null;
        String error = firstNonNullText(root, "error", "message");
        return new OperationStatus(id, status, progress, resultUrl, error, root);
    }

    private String firstNonNullText(JsonNode root, String... keys) {
        for (String k : keys) {
            if (root.hasNonNull(k)) return root.get(k).asText();
        }
        return null;
    }

}

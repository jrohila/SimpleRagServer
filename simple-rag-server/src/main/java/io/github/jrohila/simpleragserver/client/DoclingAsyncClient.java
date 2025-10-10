package io.github.jrohila.simpleragserver.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.jrohila.simpleragserver.dto.DoclingConversionRequest;
import io.github.jrohila.simpleragserver.dto.DoclingConversionResponse;
import io.github.jrohila.simpleragserver.dto.DoclingChunkRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

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
@Service
public class DoclingAsyncClient {

    private static final Logger logger = LoggerFactory.getLogger(DoclingAsyncClient.class);

    private final RestTemplate restTemplate;
    private final String doclingBaseUrl;
    private final String asyncConvertPath;
    private final ObjectMapper objectMapper;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int resultReadTimeoutMs;
    private final String statusPathTemplate;

    public DoclingAsyncClient(
            @Value("${docling-serve.url}") String doclingBaseUrl,
        @Value("${docling.async.convert-path:/v1/convert/source/async}") String asyncConvertPath,
        @Value("${docling.async.status-path-template:/v1/status/poll/{id}}") String statusPathTemplate,
            @Value("${docling.timeout.connect:10000}") int connectTimeoutMs,
            @Value("${docling.timeout.read:600000}") int readTimeoutMs,
            @Value("${docling.result.timeout.read:15000}") int resultReadTimeoutMs
    ) {
        this.doclingBaseUrl = doclingBaseUrl;
        this.asyncConvertPath = asyncConvertPath;
        this.statusPathTemplate = statusPathTemplate;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.resultReadTimeoutMs = resultReadTimeoutMs;
        this.restTemplate = createRestTemplate();
        this.objectMapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    private RestTemplate createRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory baseFactory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        baseFactory.setConnectTimeout(connectTimeoutMs);
        baseFactory.setReadTimeout(readTimeoutMs);

        org.springframework.http.client.BufferingClientHttpRequestFactory factory =
                new org.springframework.http.client.BufferingClientHttpRequestFactory(baseFactory);

        RestTemplate template = new RestTemplate();
        template.setRequestFactory(factory);
        template.getInterceptors().add((request, body, execution) -> {
            try {
                String bodyStr = new String(body, StandardCharsets.UTF_8);
                try {
                    JsonNode root = objectMapper.readTree(bodyStr);
                    JsonNode sources = root.path("sources");
                    if (sources.isArray() && sources.size() > 0) {
                        JsonNode s0 = sources.get(0);
                        String filename = s0.path("filename").asText(null);
                        String b64 = s0.path("base64_string").asText(null);
                        int len = b64 != null ? b64.length() : 0;
                        logger.debug("Docling(ASYNC) HTTP payload (redacted): filename={} base64_length={}", filename, len);
                    }
                } catch (Exception jsonEx) {
                    logger.debug("Docling(ASYNC) payload (raw, redacted omitted): {}", bodyStr.substring(0, Math.min(512, bodyStr.length())));
                }
            } catch (Exception ex) {
                logger.debug("Docling(ASYNC) HTTP payload logging failed (non-fatal)");
            }
            return execution.execute(request, body);
        });
        return template;
    }

    private RestTemplate createRestTemplate(int connectTimeout, int readTimeout) {
        org.springframework.http.client.SimpleClientHttpRequestFactory baseFactory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        baseFactory.setConnectTimeout(connectTimeout);
        baseFactory.setReadTimeout(readTimeout);

        org.springframework.http.client.BufferingClientHttpRequestFactory factory =
                new org.springframework.http.client.BufferingClientHttpRequestFactory(baseFactory);

        RestTemplate template = new RestTemplate();
        template.setRequestFactory(factory);
        template.getInterceptors().add((request, body, execution) -> execution.execute(request, body));
        return template;
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
    public StartOperationResult convertFromFile(MultipartFile file) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        DoclingConversionRequest req = DoclingConversionRequest.fromBase64(file.getOriginalFilename(), base64);
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            HttpEntity<DoclingConversionRequest> entity = new HttpEntity<>(request, headers);

            String url = doclingBaseUrl + asyncConvertPath;
            try {
                Object redacted = buildRedactedRequestLog(request);
                String payload = objectMapper.writeValueAsString(redacted);
                logger.info("Docling ASYNC request POST {} payload_redacted={}", url, payload);
            } catch (Exception ignore) {
                logger.debug("Docling ASYNC request serialization for logging failed (payload redacted)");
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            int status = response.getStatusCode().value();
            URI loc = Optional.ofNullable(response.getHeaders().getLocation()).orElse(null);
            String body = Optional.ofNullable(response.getBody()).orElse("");
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
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            logger.error("Docling ASYNC returned {} with body: {}", e.getStatusCode(), body);
            throw new RuntimeException("Docling async 4xx error: " + e.getStatusCode() + " body=" + body, e);
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
                for (DoclingConversionRequest.SourceInput s : request.getSources()) {
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
            DoclingConversionRequest.Options convertOptionsOverride
    ) {
        DoclingChunkRequest req = new DoclingChunkRequest();
        DoclingConversionRequest.SourceInput src = new DoclingConversionRequest.SourceInput();
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
            MultipartFile file,
            DoclingChunkRequest.HybridChunkerOptions hybridOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            DoclingConversionRequest.Options convertOptionsOverride
    ) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        DoclingChunkRequest req = new DoclingChunkRequest();
        DoclingConversionRequest.SourceInput src = new DoclingConversionRequest.SourceInput();
        src.setKind("file");
        src.setFilename(file.getOriginalFilename());
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
            DoclingConversionRequest.Options convertOptionsOverride
    ) {
        DoclingChunkRequest req = new DoclingChunkRequest();
        DoclingConversionRequest.SourceInput src = new DoclingConversionRequest.SourceInput();
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
            MultipartFile file,
            DoclingChunkRequest.HierarchicalChunkerOptions hierarchicalOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            DoclingConversionRequest.Options convertOptionsOverride
    ) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        DoclingChunkRequest req = new DoclingChunkRequest();
        DoclingConversionRequest.SourceInput src = new DoclingConversionRequest.SourceInput();
        src.setKind("file");
        src.setFilename(file.getOriginalFilename());
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);

            // Minimal redacted logging
            try {
                String json = objectMapper.writeValueAsString(payload);
                logger.info("Docling ASYNC start POST {} payload_size={}B", url, json.length());
            } catch (Exception ignore) {}

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            int status = response.getStatusCode().value();
            URI loc = Optional.ofNullable(response.getHeaders().getLocation()).orElse(null);
            String body = Optional.ofNullable(response.getBody()).orElse("");

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
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            logger.error("Docling ASYNC returned {} with body: {}", e.getStatusCode(), body);
            throw new RuntimeException("Docling async 4xx error: " + e.getStatusCode() + " body=" + body, e);
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
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            ResponseEntity<String> resp = restTemplate.exchange(absoluteUrl, HttpMethod.GET, new HttpEntity<Void>((Void) null, headers), String.class);
            int code = resp.getStatusCode().value();
            String body = Optional.ofNullable(resp.getBody()).orElse("");
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
                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept", "application/json");

                // Use a shorter read-timeout for result fetch to avoid long blocking
                RestTemplate shortTemplate = createRestTemplate(connectTimeoutMs, resultReadTimeoutMs);

                int attempts = 3; // small retry to bridge status/result race
                for (int i = 0; i < attempts; i++) {
                    try {
                        ResponseEntity<DoclingConversionResponse> resp = shortTemplate.exchange(
                                status.resultUrl, HttpMethod.GET, new HttpEntity<Void>((Void) null, headers), DoclingConversionResponse.class);
                        if (resp.getStatusCode().is2xxSuccessful()) {
                            return Optional.ofNullable(resp.getBody());
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
     * Final result of a polling run: terminal status plus optional conversion payload.
     */
    public record PollResult(OperationStatus status, DoclingConversionResponse response) {}

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
    private Integer firstNonNullInt(JsonNode root, String... keys) {
        for (String k : keys) {
            if (root.hasNonNull(k)) return root.get(k).asInt();
        }
        return null;
    }
}

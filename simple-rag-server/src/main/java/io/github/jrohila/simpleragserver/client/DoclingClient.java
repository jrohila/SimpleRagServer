package io.github.jrohila.simpleragserver.client;

import io.github.jrohila.simpleragserver.domain.DoclingConversionRequest;
import io.github.jrohila.simpleragserver.domain.DoclingConversionResponse;
import io.github.jrohila.simpleragserver.domain.DoclingChunkRequest;
import io.github.jrohila.simpleragserver.domain.DoclingChunkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Service
public class DoclingClient {
    
    private static final Logger logger = LoggerFactory.getLogger(DoclingClient.class);
    
    private final RestTemplate restTemplate;
    private final String doclingBaseUrl;
    private final ObjectMapper objectMapper;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    
    public DoclingClient(
            @Value("${docling-serve.url}") String doclingBaseUrl,
            @Value("${docling.timeout.connect:10000}") int connectTimeoutMs,
            @Value("${docling.timeout.read:600000}") int readTimeoutMs) {
        this.doclingBaseUrl = doclingBaseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.restTemplate = createRestTemplate();
        this.objectMapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    private RestTemplate createRestTemplate() {
        // Use SimpleClientHttpRequestFactory which supports timeout configuration
        org.springframework.http.client.SimpleClientHttpRequestFactory baseFactory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        baseFactory.setConnectTimeout(connectTimeoutMs);    // from properties
        baseFactory.setReadTimeout(readTimeoutMs);          // from properties

        // Wrap to allow reading body for logging
        org.springframework.http.client.BufferingClientHttpRequestFactory factory =
            new org.springframework.http.client.BufferingClientHttpRequestFactory(baseFactory);

        RestTemplate template = new RestTemplate();
        template.setRequestFactory(factory);
        // Interceptor to log request body (redacted)
        template.getInterceptors().add((request, body, execution) -> {
            try {
                String bodyStr = new String(body, java.nio.charset.StandardCharsets.UTF_8);
                // Attempt to redact base64_string
                try {
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(bodyStr);
                    com.fasterxml.jackson.databind.JsonNode sources = root.path("sources");
                    if (sources.isArray() && sources.size() > 0) {
                        com.fasterxml.jackson.databind.JsonNode s0 = sources.get(0);
                        String filename = s0.path("filename").asText(null);
                        String b64 = s0.path("base64_string").asText(null);
                        int len = b64 != null ? b64.length() : 0;
                        logger.debug("Docling HTTP payload (redacted): filename={} base64_length={}", filename, len);
                    }
                } catch (Exception jsonEx) {
                    logger.debug("Docling HTTP payload (raw, redacted omitted due to parse error): {}", bodyStr.substring(0, Math.min(512, bodyStr.length())));
                }
            } catch (Exception ex) {
                logger.debug("Docling HTTP payload logging failed (non-fatal)");
            }
            return execution.execute(request, body);
        });
        return template;
    }
    
    /**
     * Synchronous conversion from URL
     */
    public DoclingConversionResponse convertFromUrl(String url) {
        logger.info("Converting document from URL: {}", url);
        
        DoclingConversionRequest request = DoclingConversionRequest.fromUrl(url);
        return executeConversion(request);
    }
    
    /**
     * Synchronous conversion from uploaded file
     */
    public DoclingConversionResponse convertFromFile(MultipartFile file) throws IOException {
        logger.info("Converting uploaded file: {}", file.getOriginalFilename());
        
        // Convert file to base64
        byte[] fileBytes = file.getBytes();
        String base64Content = Base64.getEncoder().encodeToString(fileBytes);
        
        DoclingConversionRequest request = DoclingConversionRequest.fromBase64(
            file.getOriginalFilename(), 
            base64Content
        );
        return executeConversion(request);
    }
    
    /**
     * Synchronous conversion from byte array
     */
    public DoclingConversionResponse convertFromBytes(String filename, byte[] content) {
        logger.info("Converting document from bytes: {}", filename);
        
        String base64Content = Base64.getEncoder().encodeToString(content);
        DoclingConversionRequest request = DoclingConversionRequest.fromBase64(filename, base64Content);
        return executeConversion(request);
    }

    // ===================== Chunking APIs =====================
    public DoclingChunkResponse hybridChunkFromUrl(
            String url,
            DoclingChunkRequest.HybridChunkerOptions hybridOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            DoclingConversionRequest.Options convertOptionsOverride) {
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
        return executeChunk("/v1/chunk/hybrid/source", req);
    }

    public DoclingChunkResponse hierarchicalChunkFromUrl(
            String url,
            DoclingChunkRequest.HierarchicalChunkerOptions hierarchicalOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            DoclingConversionRequest.Options convertOptionsOverride) {
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
        return executeChunk("/v1/chunk/hierarchical/source", req);
    }

    public DoclingChunkResponse hybridChunkFromFile(
            MultipartFile file,
            DoclingChunkRequest.HybridChunkerOptions hybridOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            DoclingConversionRequest.Options convertOptionsOverride) throws IOException {
        String base64Content = Base64.getEncoder().encodeToString(file.getBytes());
        DoclingChunkRequest req = new DoclingChunkRequest();
        DoclingConversionRequest.SourceInput src = new DoclingConversionRequest.SourceInput();
        src.setKind("file");
        src.setFilename(file.getOriginalFilename());
        src.setBase64String(base64Content);
        req.setSources(List.of(src));
        if (convertOptionsOverride != null) req.setConvertOptions(convertOptionsOverride);
        req.setChunkingOptions(hybridOptions);
        if (includeConvertedDoc != null) req.setIncludeConvertedDoc(includeConvertedDoc);
        if (targetKind != null && !targetKind.isBlank()) {
            DoclingChunkRequest.Target t = new DoclingChunkRequest.Target();
            t.setKind(targetKind);
            req.setTarget(t);
        }
        return executeChunk("/v1/chunk/hybrid/source", req);
    }

    public DoclingChunkResponse hierarchicalChunkFromFile(
            MultipartFile file,
            DoclingChunkRequest.HierarchicalChunkerOptions hierarchicalOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            DoclingConversionRequest.Options convertOptionsOverride) throws IOException {
        String base64Content = Base64.getEncoder().encodeToString(file.getBytes());
        DoclingChunkRequest req = new DoclingChunkRequest();
        DoclingConversionRequest.SourceInput src = new DoclingConversionRequest.SourceInput();
        src.setKind("file");
        src.setFilename(file.getOriginalFilename());
        src.setBase64String(base64Content);
        req.setSources(List.of(src));
        if (convertOptionsOverride != null) req.setConvertOptions(convertOptionsOverride);
        req.setChunkingOptions(hierarchicalOptions);
        if (includeConvertedDoc != null) req.setIncludeConvertedDoc(includeConvertedDoc);
        if (targetKind != null && !targetKind.isBlank()) {
            DoclingChunkRequest.Target t = new DoclingChunkRequest.Target();
            t.setKind(targetKind);
            req.setTarget(t);
        }
        return executeChunk("/v1/chunk/hierarchical/source", req);
    }

    public DoclingChunkResponse hybridChunkFromBytes(
            String filename,
            byte[] content,
            DoclingChunkRequest.HybridChunkerOptions hybridOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            DoclingConversionRequest.Options convertOptionsOverride) {
        String base64Content = Base64.getEncoder().encodeToString(content);
        DoclingChunkRequest req = new DoclingChunkRequest();
        DoclingConversionRequest.SourceInput src = new DoclingConversionRequest.SourceInput();
        src.setKind("file");
        src.setFilename(filename);
        src.setBase64String(base64Content);
        req.setSources(List.of(src));
        if (convertOptionsOverride != null) req.setConvertOptions(convertOptionsOverride);
        req.setChunkingOptions(hybridOptions);
        if (includeConvertedDoc != null) req.setIncludeConvertedDoc(includeConvertedDoc);
        if (targetKind != null && !targetKind.isBlank()) {
            DoclingChunkRequest.Target t = new DoclingChunkRequest.Target();
            t.setKind(targetKind);
            req.setTarget(t);
        }
        return executeChunk("/v1/chunk/hybrid/source", req);
    }

    private DoclingChunkResponse executeChunk(String path, DoclingChunkRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            // Pre-flight guard: ensure base64_string exists when kind=file
            try {
                String actualJson = objectMapper.writeValueAsString(request);
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(actualJson);
                com.fasterxml.jackson.databind.JsonNode sources = root.path("sources");
                if (sources.isArray() && sources.size() > 0) {
                    com.fasterxml.jackson.databind.JsonNode s0 = sources.get(0);
                    String kind = s0.path("kind").asText("");
                    if ("file".equalsIgnoreCase(kind)) {
                        boolean missing = s0.path("base64_string").isMissingNode() || s0.path("base64_string").isNull();
                        if (missing) {
                            logger.error("Docling chunk request missing required field sources[0].base64_string for kind=file");
                            throw new IllegalArgumentException("Missing required field: sources[0].base64_string");
                        }
                    }
                }
            } catch (IllegalArgumentException iae) {
                throw iae;
            } catch (Exception ex) {
                logger.debug("Docling pre-flight payload validation skipped due to parse error (non-fatal)");
            }

            HttpEntity<DoclingChunkRequest> entity = new HttpEntity<>(request, headers);

            String url = doclingBaseUrl + path;
            // Log with redaction for base64
            try {
                String actualJson = objectMapper.writeValueAsString(request);
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(actualJson);
                com.fasterxml.jackson.databind.JsonNode sources = root.path("sources");
                String filename = null; int len = 0; String kind = null;
                if (sources.isArray() && sources.size() > 0) {
                    com.fasterxml.jackson.databind.JsonNode s0 = sources.get(0);
                    kind = s0.path("kind").asText(null);
                    filename = s0.path("filename").asText(null);
                    String b64 = s0.path("base64_string").asText(null);
                    len = b64 != null ? b64.length() : 0;
                }
                logger.info("Docling chunk request POST {} kind={} filename={} base64_length={}", url, kind, filename, len);
            } catch (Exception ignore) {}

            ResponseEntity<DoclingChunkResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                DoclingChunkResponse.class
            );

            DoclingChunkResponse result = response.getBody();
            if (result == null) {
                throw new RuntimeException("Empty response from Docling chunk service");
            }
            logger.info("Docling chunking completed. chunks={} includeConvertedDoc={}",
                    result.getChunks() != null ? result.getChunks().size() : 0,
                    request.getIncludeConvertedDoc());
            return result;
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            logger.error("Docling returned {} with body: {}", e.getStatusCode(), body);
            throw new RuntimeException("Docling 4xx error: " + e.getStatusCode() + " body=" + body, e);
        } catch (Exception e) {
            logger.error("Failed to chunk document using Docling service", e);
            throw new RuntimeException("Document chunking failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Execute the actual HTTP request to Docling Serve
     */
    private DoclingConversionResponse executeConversion(DoclingConversionRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            
            HttpEntity<DoclingConversionRequest> entity = new HttpEntity<>(request, headers);
            
            String url = doclingBaseUrl + "/v1/convert/source";
            try {
                Object redacted = buildRedactedRequestLog(request);
                String payload = objectMapper.writeValueAsString(redacted);
                logger.info("Docling request POST {} payload_redacted={}", url, payload);
                // Extra diagnostic: verify serialized payload contains expected keys
                try {
                    String actualJson = objectMapper.writeValueAsString(request);
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(actualJson);
                    com.fasterxml.jackson.databind.JsonNode sources = root.path("sources");
                    boolean hasSources = sources.isArray() && sources.size() > 0;
                    String kind = hasSources ? sources.get(0).path("kind").asText(null) : null;
                    com.fasterxml.jackson.databind.JsonNode s0 = hasSources ? sources.get(0) : null;
                    boolean hasFilename = s0 != null && s0.has("filename");
                    boolean hasB64 = s0 != null && s0.has("base64_string") && !s0.path("base64_string").isNull();
                    String filename = (hasFilename && s0 != null) ? s0.path("filename").asText(null) : null;
                    logger.info("Docling payload check: hasSources={} kind={} hasFilename={} hasBase64StringKey={} filename={} jsonLength={}", 
                        hasSources, kind, hasFilename, hasB64, filename, actualJson.length());
                } catch (Exception diagEx) {
                    logger.debug("Docling payload self-check failed (non-fatal)", diagEx);
                }
            } catch (Exception ignore) {
                logger.debug("Docling request serialization for logging failed (payload redacted)");
            }
            
            // Pre-flight guard: ensure base64_string exists when kind=file
            try {
                String actualJson = objectMapper.writeValueAsString(request);
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(actualJson);
                com.fasterxml.jackson.databind.JsonNode sources = root.path("sources");
                if (sources.isArray() && sources.size() > 0) {
                    com.fasterxml.jackson.databind.JsonNode s0 = sources.get(0);
                    String kind = s0.path("kind").asText("");
                    if ("file".equalsIgnoreCase(kind)) {
                        boolean missing = s0.path("base64_string").isMissingNode() || s0.path("base64_string").isNull();
                        if (missing) {
                            logger.error("Docling request missing required field sources[0].base64_string for kind=file");
                            throw new IllegalArgumentException("Missing required field: sources[0].base64_string");
                        }
                    }
                }
            } catch (IllegalArgumentException iae) {
                throw iae;
            } catch (Exception ex) {
                logger.debug("Docling pre-flight payload validation skipped due to parse error (non-fatal)");
            }

            ResponseEntity<DoclingConversionResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                DoclingConversionResponse.class
            );
            
            DoclingConversionResponse result = response.getBody();
            if (result == null) {
                throw new RuntimeException("Empty response from Docling service");
            }
            
            logger.info("Docling conversion completed. Status: {}, Processing time: {}s", 
                result.getStatus(), result.getProcessingTime());
            
            if (!result.isSuccess()) {
                logger.warn("Docling conversion completed with warnings. Errors: {}", result.getErrors());
            }
            
            return result;
            
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            logger.error("Docling returned {} with body: {}", e.getStatusCode(), body);
            throw new RuntimeException("Docling 4xx error: " + e.getStatusCode() + " body=" + body, e);
        } catch (Exception e) {
            logger.error("Failed to convert document using Docling service", e);
            throw new RuntimeException("Document conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build a redacted view of the request for logging purposes (no base64 content).
     */
    private Object buildRedactedRequestLog(DoclingConversionRequest request) {
        Map<String, Object> out = new HashMap<>();
        try {
            if (request.getOptions() != null) {
                out.put("to_formats", request.getOptions().getToFormats());
                out.put("from_formats", request.getOptions().getFromFormats());
                out.put("pipeline", request.getOptions().getPipeline());
                out.put("do_ocr", request.getOptions().getDoOcr());
                out.put("do_table_structure", request.getOptions().getDoTableStructure());
                out.put("table_mode", request.getOptions().getTableMode());
                out.put("abort_on_error", request.getOptions().getAbortOnError());
            }

            List<Map<String, Object>> sources = new ArrayList<>();
            if (request.getSources() != null) {
                for (DoclingConversionRequest.SourceInput s : request.getSources()) {
                    Map<String, Object> sm = new HashMap<>();
                    sm.put("kind", s.getKind());
                    if (s.getUrl() != null) sm.put("url", s.getUrl());
                    if (s.getFilename() != null) sm.put("filename", s.getFilename());
                    if (s.getBase64String() != null) {
                        sm.put("base64_length", s.getBase64String().length());
                        sm.put("base64_string", "[redacted]");
                    }
                    sources.add(sm);
                }
            }
            out.put("sources", sources);
        } catch (Exception ex) {
            // In case of any unexpected issue, fall back to minimal info
            out.put("info", "redaction_failed");
        }
        return out;
    }
    
    /**
     * Health check for Docling service
     */
    public boolean isHealthy() {
        try {
            String healthUrl = doclingBaseUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Docling health check failed", e);
            return false;
        }
    }
}
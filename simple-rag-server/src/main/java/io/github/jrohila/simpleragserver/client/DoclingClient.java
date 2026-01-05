package io.github.jrohila.simpleragserver.client;

import io.github.jrohila.simpleragserver.domain.DoclingConversionRequest;
import io.github.jrohila.simpleragserver.domain.Options;
import io.github.jrohila.simpleragserver.domain.DoclingConversionResponse;
import io.github.jrohila.simpleragserver.domain.DoclingChunkRequest;
import io.github.jrohila.simpleragserver.domain.DoclingChunkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import jakarta.inject.Singleton;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.jrohila.simpleragserver.domain.SourceInput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.micronaut.http.multipart.CompletedFileUpload;

import java.io.IOException;
import java.util.Base64;
import java.time.Duration;

@Singleton
public class DoclingClient {
    
    private static final Logger logger = LoggerFactory.getLogger(DoclingClient.class);
    
    private final HttpClient httpClient;
    private final String doclingBaseUrl;
    private final ObjectMapper objectMapper;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    
    public DoclingClient(
            @Property(name = "docling-serve.url") String doclingBaseUrl,
            @Property(name = "docling.timeout.connect", defaultValue = "10000") int connectTimeoutMs,
            @Property(name = "docling.timeout.read", defaultValue = "600000") int readTimeoutMs,
            HttpClient httpClient) {
        this.doclingBaseUrl = doclingBaseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
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
    public DoclingConversionResponse convertFromFile(CompletedFileUpload file) throws IOException {
        logger.info("Converting uploaded file: {}", file.getFilename());
        
        // Convert file to base64
        byte[] fileBytes = file.getBytes();
        String base64Content = Base64.getEncoder().encodeToString(fileBytes);
        
        DoclingConversionRequest request = DoclingConversionRequest.fromBase64(
            file.getFilename(), 
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
            Options convertOptionsOverride) {
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
        return executeChunk("/v1/chunk/hybrid/source", req);
    }

        public DoclingChunkResponse hierarchicalChunkFromUrl(
            String url,
            DoclingChunkRequest.HierarchicalChunkerOptions hierarchicalOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            Options convertOptionsOverride) {
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
        return executeChunk("/v1/chunk/hierarchical/source", req);
    }

        public DoclingChunkResponse hybridChunkFromFile(
            CompletedFileUpload file,
            DoclingChunkRequest.HybridChunkerOptions hybridOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            Options convertOptionsOverride) throws IOException {
        String base64Content = Base64.getEncoder().encodeToString(file.getBytes());
        DoclingChunkRequest req = new DoclingChunkRequest();
        SourceInput src = new SourceInput();
        src.setKind("file");
        src.setFilename(file.getFilename());
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
            CompletedFileUpload file,
            DoclingChunkRequest.HierarchicalChunkerOptions hierarchicalOptions,
            Boolean includeConvertedDoc,
            String targetKind,
            Options convertOptionsOverride) throws IOException {
        String base64Content = Base64.getEncoder().encodeToString(file.getBytes());
        DoclingChunkRequest req = new DoclingChunkRequest();
        SourceInput src = new SourceInput();
        src.setKind("file");
        src.setFilename(file.getFilename());
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
            Options convertOptionsOverride) {
        String base64Content = Base64.getEncoder().encodeToString(content);
        DoclingChunkRequest req = new DoclingChunkRequest();
        SourceInput src = new SourceInput();
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

            HttpRequest<DoclingChunkRequest> httpRequest = HttpRequest.POST(url, request)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE);
            
            HttpResponse<DoclingChunkResponse> response = httpClient.toBlocking().exchange(
                httpRequest,
                DoclingChunkResponse.class
            );

            DoclingChunkResponse result = response.body();
            if (result == null) {
                throw new RuntimeException("Empty response from Docling chunk service");
            }
            logger.info("Docling chunking completed. chunks={} includeConvertedDoc={}",
                    result.getChunks() != null ? result.getChunks().size() : 0,
                    request.getIncludeConvertedDoc());
            return result;
        } catch (HttpClientException e) {
            logger.error("Docling HTTP error: {}", e.getMessage());
            throw new RuntimeException("Docling HTTP error: " + e.getMessage(), e);
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

            HttpRequest<DoclingConversionRequest> httpRequest = HttpRequest.POST(url, request)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE);
            
            HttpResponse<DoclingConversionResponse> response = httpClient.toBlocking().exchange(
                httpRequest,
                DoclingConversionResponse.class
            );
            
            DoclingConversionResponse result = response.body();
            if (result == null) {
                throw new RuntimeException("Empty response from Docling service");
            }
            
            logger.info("Docling conversion completed. Status: {}, Processing time: {}s", 
                result.getStatus(), result.getProcessingTime());
            
            if (!result.isSuccess()) {
                logger.warn("Docling conversion completed with warnings. Errors: {}", result.getErrors());
            }
            
            return result;
            
        } catch (HttpClientException e) {
            logger.error("Docling HTTP error: {}", e.getMessage());
            throw new RuntimeException("Docling HTTP error: " + e.getMessage(), e);
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
                for (SourceInput s : request.getSources()) {
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
            HttpRequest<?> httpRequest = HttpRequest.GET(healthUrl);
            HttpResponse<String> response = httpClient.toBlocking().exchange(httpRequest, String.class);
            return response.getStatus().getCode() >= 200 && response.getStatus().getCode() < 300;
        } catch (Exception e) {
            logger.warn("Docling health check failed", e);
            return false;
        }
    }
}
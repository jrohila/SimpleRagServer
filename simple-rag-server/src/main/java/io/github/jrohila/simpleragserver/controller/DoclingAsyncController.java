package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.client.DoclingAsyncClient;
import io.github.jrohila.simpleragserver.client.DoclingAsyncClient.OperationStatus;
import io.github.jrohila.simpleragserver.client.DoclingAsyncClient.StartOperationResult;
import io.github.jrohila.simpleragserver.dto.DoclingChunkRequest;
import io.github.jrohila.simpleragserver.dto.DoclingConversionResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/docling/async")
public class DoclingAsyncController {

    private final DoclingAsyncClient asyncClient;

    public DoclingAsyncController(DoclingAsyncClient asyncClient) {
        this.asyncClient = asyncClient;
    }

    // Start async conversion from URL
    @PostMapping("/convert/url")
    public ResponseEntity<StartOperationResult> startFromUrl(@RequestParam("url") String url) {
        StartOperationResult start = asyncClient.convertFromUrl(url);
        String internalPoll = "/api/docling/async/operations/" + start.operationId();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(internalPoll));
        // Return a body where pollUrl points to our internal operations endpoint (ID-based)
        StartOperationResult body = new StartOperationResult(start.operationId(), internalPoll, start.httpStatus(), start.rawBody());
        return new ResponseEntity<>(body, headers, HttpStatus.ACCEPTED);
    }

    // Start async conversion from file upload
    @PostMapping(path = "/convert/file", consumes = "multipart/form-data")
    public ResponseEntity<StartOperationResult> startFromFile(@RequestParam("file") MultipartFile file) throws IOException {
        StartOperationResult start = asyncClient.convertFromFile(file);
        String internalPoll = "/api/docling/async/operations/" + start.operationId();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(internalPoll));
        StartOperationResult body = new StartOperationResult(start.operationId(), internalPoll, start.httpStatus(), start.rawBody());
        return new ResponseEntity<>(body, headers, HttpStatus.ACCEPTED);
    }

    // Check status by operation id
    @GetMapping("/operations/{id}")
    public OperationStatus getStatusById(@PathVariable("id") String id) {
        return asyncClient.getStatusById(id);
    }

    // Check status by full poll URL (useful if Location header was stored by client)
    // Check status by operation id (preferred)
    @GetMapping("/status")
    public OperationStatus getStatusByIdQuery(@RequestParam(value = "id", required = true) String id) {
        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Missing required query parameter 'id'");
            }
            return asyncClient.getStatusById(id.trim());
        } catch (Exception ex) {
            // Return a synthetic status object conveying the error rather than 500
            return new OperationStatus(null, "error", null, null,
                    ex.getMessage(), null);
        }
    }

    // Fetch result for a completed operation by id (if available)
    @GetMapping("/operations/{id}/result")
    public ResponseEntity<?> getResultById(
        @PathVariable("id") String id,
        @RequestParam(value = "redirect", defaultValue = "true") boolean redirect
    ) {
        OperationStatus st = asyncClient.getStatusById(id);
        if (!st.isTerminal()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    "status", st.status(),
                    "message", "Operation not complete yet"
            ));
        }
        if (!st.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "status", st.status(),
                    "error", st.error()
            ));
        }
    // If user wants non-blocking behavior, redirect to Docling's result endpoint
    if (redirect && st.resultUrl() != null && !st.resultUrl().isBlank()) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
            .location(URI.create(st.resultUrl()))
            .build();
    }
    // Otherwise, proxy the result (may take some time depending on payload size)
    return asyncClient.fetchResult(st)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    // ===== Async CHUNK: Hybrid (URL) =====
    @PostMapping("/chunk/hybrid/url")
    public ResponseEntity<StartOperationResult> startHybridChunkFromUrl(
            @RequestParam("url") String url,
            @RequestParam(value = "use_markdown_tables", required = false) Boolean useMarkdownTables,
            @RequestParam(value = "include_raw_text", required = false) Boolean includeRawText,
            @RequestParam(value = "max_tokens", required = false) Integer maxTokens,
            @RequestParam(value = "tokenizer", required = false) String tokenizer,
            @RequestParam(value = "merge_peers", required = false) Boolean mergePeers,
            @RequestParam(value = "include_converted_doc", required = false) Boolean includeConvertedDoc,
            @RequestParam(value = "target_kind", required = false) String targetKind,
            @RequestParam(value = "to_formats", required = false) List<String> toFormats,
            @RequestParam(value = "do_ocr", required = false) Boolean doOcr,
            @RequestParam(value = "do_table_structure", required = false) Boolean doTableStructure,
            @RequestParam(value = "table_mode", required = false) String tableMode,
            @RequestParam(value = "pipeline", required = false) String pipeline
    ) {
        DoclingChunkRequest.HybridChunkerOptions opts = new DoclingChunkRequest.HybridChunkerOptions();
        if (useMarkdownTables != null) opts.setUseMarkdownTables(useMarkdownTables);
        if (includeRawText != null) opts.setIncludeRawText(includeRawText);
        if (maxTokens != null) opts.setMaxTokens(maxTokens);
        if (tokenizer != null && !tokenizer.isBlank()) opts.setTokenizer(tokenizer);
        if (mergePeers != null) opts.setMergePeers(mergePeers);

        var conv = buildConvertOptions(toFormats, doOcr, doTableStructure, tableMode, pipeline);
        StartOperationResult start = asyncClient.hybridChunkFromUrl(url, opts, includeConvertedDoc, targetKind, conv);
        String internalPoll = "/api/docling/async/operations/" + start.operationId();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(internalPoll));
        StartOperationResult body = new StartOperationResult(start.operationId(), internalPoll, start.httpStatus(), start.rawBody());
        return new ResponseEntity<>(body, headers, HttpStatus.ACCEPTED);
    }

    // ===== Async CHUNK: Hybrid (File) =====
    @PostMapping(path = "/chunk/hybrid/file", consumes = "multipart/form-data")
    public ResponseEntity<StartOperationResult> startHybridChunkFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "use_markdown_tables", required = false) Boolean useMarkdownTables,
            @RequestParam(value = "include_raw_text", required = false) Boolean includeRawText,
            @RequestParam(value = "max_tokens", required = false) Integer maxTokens,
            @RequestParam(value = "tokenizer", required = false) String tokenizer,
            @RequestParam(value = "merge_peers", required = false) Boolean mergePeers,
            @RequestParam(value = "include_converted_doc", required = false) Boolean includeConvertedDoc,
            @RequestParam(value = "target_kind", required = false) String targetKind,
            @RequestParam(value = "to_formats", required = false) List<String> toFormats,
            @RequestParam(value = "do_ocr", required = false) Boolean doOcr,
            @RequestParam(value = "do_table_structure", required = false) Boolean doTableStructure,
            @RequestParam(value = "table_mode", required = false) String tableMode,
            @RequestParam(value = "pipeline", required = false) String pipeline
    ) throws IOException {
        DoclingChunkRequest.HybridChunkerOptions opts = new DoclingChunkRequest.HybridChunkerOptions();
        if (useMarkdownTables != null) opts.setUseMarkdownTables(useMarkdownTables);
        if (includeRawText != null) opts.setIncludeRawText(includeRawText);
        if (maxTokens != null) opts.setMaxTokens(maxTokens);
        if (tokenizer != null && !tokenizer.isBlank()) opts.setTokenizer(tokenizer);
        if (mergePeers != null) opts.setMergePeers(mergePeers);

        var conv = buildConvertOptions(toFormats, doOcr, doTableStructure, tableMode, pipeline);
        StartOperationResult start = asyncClient.hybridChunkFromFile(file, opts, includeConvertedDoc, targetKind, conv);
        String internalPoll = "/api/docling/async/operations/" + start.operationId();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(internalPoll));
        StartOperationResult body = new StartOperationResult(start.operationId(), internalPoll, start.httpStatus(), start.rawBody());
        return new ResponseEntity<>(body, headers, HttpStatus.ACCEPTED);
    }

    // ===== Async CHUNK: Hierarchical (URL) =====
    @PostMapping("/chunk/hierarchical/url")
    public ResponseEntity<StartOperationResult> startHierarchicalChunkFromUrl(
            @RequestParam("url") String url,
            @RequestParam(value = "use_markdown_tables", required = false) Boolean useMarkdownTables,
            @RequestParam(value = "include_raw_text", required = false) Boolean includeRawText,
            @RequestParam(value = "include_converted_doc", required = false) Boolean includeConvertedDoc,
            @RequestParam(value = "target_kind", required = false) String targetKind,
            @RequestParam(value = "to_formats", required = false) List<String> toFormats,
            @RequestParam(value = "do_ocr", required = false) Boolean doOcr,
            @RequestParam(value = "do_table_structure", required = false) Boolean doTableStructure,
            @RequestParam(value = "table_mode", required = false) String tableMode,
            @RequestParam(value = "pipeline", required = false) String pipeline
    ) {
        DoclingChunkRequest.HierarchicalChunkerOptions opts = new DoclingChunkRequest.HierarchicalChunkerOptions();
        if (useMarkdownTables != null) opts.setUseMarkdownTables(useMarkdownTables);
        if (includeRawText != null) opts.setIncludeRawText(includeRawText);

        var conv = buildConvertOptions(toFormats, doOcr, doTableStructure, tableMode, pipeline);
        StartOperationResult start = asyncClient.hierarchicalChunkFromUrl(url, opts, includeConvertedDoc, targetKind, conv);
        String internalPoll = "/api/docling/async/operations/" + start.operationId();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(internalPoll));
        StartOperationResult body = new StartOperationResult(start.operationId(), internalPoll, start.httpStatus(), start.rawBody());
        return new ResponseEntity<>(body, headers, HttpStatus.ACCEPTED);
    }

    // ===== Async CHUNK: Hierarchical (File) =====
    @PostMapping(path = "/chunk/hierarchical/file", consumes = "multipart/form-data")
    public ResponseEntity<StartOperationResult> startHierarchicalChunkFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "use_markdown_tables", required = false) Boolean useMarkdownTables,
            @RequestParam(value = "include_raw_text", required = false) Boolean includeRawText,
            @RequestParam(value = "include_converted_doc", required = false) Boolean includeConvertedDoc,
            @RequestParam(value = "target_kind", required = false) String targetKind,
            @RequestParam(value = "to_formats", required = false) List<String> toFormats,
            @RequestParam(value = "do_ocr", required = false) Boolean doOcr,
            @RequestParam(value = "do_table_structure", required = false) Boolean doTableStructure,
            @RequestParam(value = "table_mode", required = false) String tableMode,
            @RequestParam(value = "pipeline", required = false) String pipeline
    ) throws IOException {
        DoclingChunkRequest.HierarchicalChunkerOptions opts = new DoclingChunkRequest.HierarchicalChunkerOptions();
        if (useMarkdownTables != null) opts.setUseMarkdownTables(useMarkdownTables);
        if (includeRawText != null) opts.setIncludeRawText(includeRawText);

        var conv = buildConvertOptions(toFormats, doOcr, doTableStructure, tableMode, pipeline);
        StartOperationResult start = asyncClient.hierarchicalChunkFromFile(file, opts, includeConvertedDoc, targetKind, conv);
        String internalPoll = "/api/docling/async/operations/" + start.operationId();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(internalPoll));
        StartOperationResult body = new StartOperationResult(start.operationId(), internalPoll, start.httpStatus(), start.rawBody());
        return new ResponseEntity<>(body, headers, HttpStatus.ACCEPTED);
    }

    // ---- helpers ----
    private static String extractIdFromUrl(String url) {
        if (url == null) return null;
        int i = url.lastIndexOf('/');
        return i >= 0 ? url.substring(i + 1) : url;
    }

    private static io.github.jrohila.simpleragserver.dto.DoclingConversionRequest.Options buildConvertOptions(
            java.util.List<String> toFormats,
            java.lang.Boolean doOcr,
            java.lang.Boolean doTableStructure,
            java.lang.String tableMode,
            java.lang.String pipeline
    ) {
        io.github.jrohila.simpleragserver.dto.DoclingConversionRequest.Options conv = null;
        if ((toFormats != null && !toFormats.isEmpty()) || doOcr != null || doTableStructure != null || tableMode != null || pipeline != null) {
            conv = new io.github.jrohila.simpleragserver.dto.DoclingConversionRequest.Options();
            if (toFormats != null && !toFormats.isEmpty()) conv.setToFormats(toFormats);
            if (doOcr != null) conv.setDoOcr(doOcr);
            if (doTableStructure != null) conv.setDoTableStructure(doTableStructure);
            if (tableMode != null && !tableMode.isBlank()) conv.setTableMode(tableMode);
            if (pipeline != null && !pipeline.isBlank()) conv.setPipeline(pipeline);
        }
        return conv;
    }
}

package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.client.DoclingAsyncClient;
import io.github.jrohila.simpleragserver.client.DoclingAsyncClient.OperationStatus;
import io.github.jrohila.simpleragserver.client.DoclingAsyncClient.StartOperationResult;
import io.github.jrohila.simpleragserver.domain.DoclingChunkRequest;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.multipart.CompletedFileUpload;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Controller("/api/docling/async")
public class DoclingAsyncController {

    private final DoclingAsyncClient asyncClient;

    public DoclingAsyncController(DoclingAsyncClient asyncClient) {
        this.asyncClient = asyncClient;
    }

    // Start async conversion from URL
    @Post(uri = "/convert/url")
    public HttpResponse<StartOperationResult> startFromUrl(@QueryValue("url") String url) {
        StartOperationResult start = asyncClient.convertFromUrl(url);
        String internalPoll = "/api/docling/async/operations/" + start.operationId();
        // Return a body where pollUrl points to our internal operations endpoint (ID-based)
        StartOperationResult body = new StartOperationResult(start.operationId(), internalPoll, start.httpStatus(), start.rawBody());
        return HttpResponse.status(HttpStatus.ACCEPTED).header(HttpHeaders.LOCATION, internalPoll).body(body);
    }

    // Start async conversion from file upload
    @Post(uri = "/convert/file", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse<StartOperationResult> startFromFile(@Part("file") CompletedFileUpload file) throws IOException {
        StartOperationResult start = asyncClient.convertFromFile(file);
        String internalPoll = "/api/docling/async/operations/" + start.operationId();
        StartOperationResult body = new StartOperationResult(start.operationId(), internalPoll, start.httpStatus(), start.rawBody());
        return HttpResponse.status(HttpStatus.ACCEPTED).header(HttpHeaders.LOCATION, internalPoll).body(body);
    }

    // Check status by operation id
    @Get(uri = "/operations/{id}")
    public OperationStatus getStatusById(@PathVariable("id") String id) {
        return asyncClient.getStatusById(id);
    }

    // Check status by full poll URL (useful if Location header was stored by client)
    // Check status by operation id (preferred)
    @Get(uri = "/status")
    public OperationStatus getStatusByIdQuery(@QueryValue(value = "id") String id) {
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
    @Get(uri = "/operations/{id}/result")
    public HttpResponse<?> getResultById(
        @PathVariable("id") String id,
        @QueryValue(value = "redirect", defaultValue = "true") boolean redirect
    ) {
        OperationStatus st = asyncClient.getStatusById(id);
        if (!st.isTerminal()) {
            return HttpResponse.status(HttpStatus.ACCEPTED).body(Map.of(
                    "status", st.status(),
                    "message", "Operation not complete yet"
            ));
        }
        if (!st.isSuccess()) {
            return HttpResponse.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "status", st.status(),
                    "error", st.error()
            ));
        }
        // If user wants non-blocking behavior, redirect to Docling's result endpoint
        if (redirect && st.resultUrl() != null && !st.resultUrl().isBlank()) {
            return HttpResponse.status(HttpStatus.SEE_OTHER).header(HttpHeaders.LOCATION, st.resultUrl());
        }
        // Otherwise, proxy the result (may take some time depending on payload size)
        return asyncClient.fetchResult(st)
            .<HttpResponse<?>>map(HttpResponse::ok)
            .orElseGet(() -> HttpResponse.status(HttpStatus.NO_CONTENT));
    }

    // ===== Async CHUNK: Hybrid (URL) =====
        @Post(uri = "/chunk/hybrid/url")
        public HttpResponse<StartOperationResult> startHybridChunkFromUrl(
            @QueryValue("url") String url,
            @QueryValue(value = "use_markdown_tables") Boolean useMarkdownTables,
            @QueryValue(value = "include_raw_text") Boolean includeRawText,
            @QueryValue(value = "max_tokens") Integer maxTokens,
            @QueryValue(value = "tokenizer") String tokenizer,
            @QueryValue(value = "merge_peers") Boolean mergePeers,
            @QueryValue(value = "include_converted_doc") Boolean includeConvertedDoc,
            @QueryValue(value = "target_kind") String targetKind,
            @QueryValue(value = "to_formats") List<String> toFormats,
            @QueryValue(value = "do_ocr") Boolean doOcr,
            @QueryValue(value = "do_table_structure") Boolean doTableStructure,
            @QueryValue(value = "table_mode") String tableMode,
            @QueryValue(value = "pipeline") String pipeline
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
        StartOperationResult body = new StartOperationResult(start.operationId(), internalPoll, start.httpStatus(), start.rawBody());
        return HttpResponse.status(HttpStatus.ACCEPTED).header(HttpHeaders.LOCATION, internalPoll).body(body);
    }

    // ===== Async CHUNK: Hybrid (File) =====
        @Post(uri = "/chunk/hybrid/file", consumes = MediaType.MULTIPART_FORM_DATA)
        public HttpResponse<StartOperationResult> startHybridChunkFromFile(
            @Part("file") CompletedFileUpload file,
            @QueryValue(value = "use_markdown_tables") Boolean useMarkdownTables,
            @QueryValue(value = "include_raw_text") Boolean includeRawText,
            @QueryValue(value = "max_tokens") Integer maxTokens,
            @QueryValue(value = "tokenizer") String tokenizer,
            @QueryValue(value = "merge_peers") Boolean mergePeers,
            @QueryValue(value = "include_converted_doc") Boolean includeConvertedDoc,
            @QueryValue(value = "target_kind") String targetKind,
            @QueryValue(value = "to_formats") List<String> toFormats,
            @QueryValue(value = "do_ocr") Boolean doOcr,
            @QueryValue(value = "do_table_structure") Boolean doTableStructure,
            @QueryValue(value = "table_mode") String tableMode,
            @QueryValue(value = "pipeline") String pipeline
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
        StartOperationResult body = new StartOperationResult(start.operationId(), internalPoll, start.httpStatus(), start.rawBody());
        return HttpResponse.status(HttpStatus.ACCEPTED).header(HttpHeaders.LOCATION, internalPoll).body(body);
    }

    // ===== Async CHUNK: Hierarchical (URL) =====
        @Post(uri = "/chunk/hierarchical/url")
        public HttpResponse<StartOperationResult> startHierarchicalChunkFromUrl(
            @QueryValue("url") String url,
            @QueryValue(value = "use_markdown_tables") Boolean useMarkdownTables,
            @QueryValue(value = "include_raw_text") Boolean includeRawText,
            @QueryValue(value = "include_converted_doc") Boolean includeConvertedDoc,
            @QueryValue(value = "target_kind") String targetKind,
            @QueryValue(value = "to_formats") List<String> toFormats,
            @QueryValue(value = "do_ocr") Boolean doOcr,
            @QueryValue(value = "do_table_structure") Boolean doTableStructure,
            @QueryValue(value = "table_mode") String tableMode,
            @QueryValue(value = "pipeline") String pipeline
        ) {
        DoclingChunkRequest.HierarchicalChunkerOptions opts = new DoclingChunkRequest.HierarchicalChunkerOptions();
        if (useMarkdownTables != null) opts.setUseMarkdownTables(useMarkdownTables);
        if (includeRawText != null) opts.setIncludeRawText(includeRawText);

        var conv = buildConvertOptions(toFormats, doOcr, doTableStructure, tableMode, pipeline);
        StartOperationResult start = asyncClient.hierarchicalChunkFromUrl(url, opts, includeConvertedDoc, targetKind, conv);
        String internalPoll = "/api/docling/async/operations/" + start.operationId();
        StartOperationResult body = new StartOperationResult(start.operationId(), internalPoll, start.httpStatus(), start.rawBody());
        return HttpResponse.status(HttpStatus.ACCEPTED).header(HttpHeaders.LOCATION, internalPoll).body(body);
    }

    // ===== Async CHUNK: Hierarchical (File) =====
    @Post(uri = "/chunk/hierarchical/file", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse<StartOperationResult> startHierarchicalChunkFromFile(
            @Part("file") CompletedFileUpload file,
            @QueryValue(value = "use_markdown_tables") Boolean useMarkdownTables,
            @QueryValue(value = "include_raw_text") Boolean includeRawText,
            @QueryValue(value = "include_converted_doc") Boolean includeConvertedDoc,
            @QueryValue(value = "target_kind") String targetKind,
            @QueryValue(value = "to_formats") List<String> toFormats,
            @QueryValue(value = "do_ocr") Boolean doOcr,
            @QueryValue(value = "do_table_structure") Boolean doTableStructure,
            @QueryValue(value = "table_mode") String tableMode,
            @QueryValue(value = "pipeline") String pipeline
    ) throws IOException {
        DoclingChunkRequest.HierarchicalChunkerOptions opts = new DoclingChunkRequest.HierarchicalChunkerOptions();
        if (useMarkdownTables != null) opts.setUseMarkdownTables(useMarkdownTables);
        if (includeRawText != null) opts.setIncludeRawText(includeRawText);

        var conv = buildConvertOptions(toFormats, doOcr, doTableStructure, tableMode, pipeline);
        StartOperationResult start = asyncClient.hierarchicalChunkFromFile(file, opts, includeConvertedDoc, targetKind, conv);
        String internalPoll = "/api/docling/async/operations/" + start.operationId();
        StartOperationResult body = new StartOperationResult(start.operationId(), internalPoll, start.httpStatus(), start.rawBody());
        return HttpResponse.status(HttpStatus.ACCEPTED).header(HttpHeaders.LOCATION, internalPoll).body(body);
    }

    private static io.github.jrohila.simpleragserver.domain.Options buildConvertOptions(
            java.util.List<String> toFormats,
            java.lang.Boolean doOcr,
            java.lang.Boolean doTableStructure,
            java.lang.String tableMode,
            java.lang.String pipeline
    ) {
        io.github.jrohila.simpleragserver.domain.Options conv = null;
        if ((toFormats != null && !toFormats.isEmpty()) || doOcr != null || doTableStructure != null || tableMode != null || pipeline != null) {
            conv = new io.github.jrohila.simpleragserver.domain.Options();
            if (toFormats != null && !toFormats.isEmpty()) conv.setToFormats(toFormats);
            if (doOcr != null) conv.setDoOcr(doOcr);
            if (doTableStructure != null) conv.setDoTableStructure(doTableStructure);
            if (tableMode != null && !tableMode.isBlank()) conv.setTableMode(tableMode);
            if (pipeline != null && !pipeline.isBlank()) conv.setPipeline(pipeline);
        }
        return conv;
    }
}

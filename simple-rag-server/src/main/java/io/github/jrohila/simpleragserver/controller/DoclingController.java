package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.client.DoclingClient;
import io.github.jrohila.simpleragserver.domain.DoclingConversionResponse;
import io.github.jrohila.simpleragserver.domain.DoclingChunkRequest;
import io.github.jrohila.simpleragserver.domain.DoclingChunkResponse;
import io.github.jrohila.simpleragserver.domain.Options;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;

import java.io.IOException;
import java.util.Map;
 

@Controller("/api/docling")
@Tag(name = "Docling", description = "Endpoints to convert and chunk documents using Docling Serve")
public class DoclingController {
    
    private static final Logger logger = LoggerFactory.getLogger(DoclingController.class);
    
    private final DoclingClient doclingClient;
    
    public DoclingController(DoclingClient doclingClient) {
        this.doclingClient = doclingClient;
    }
    
    /**
     * Convert document from URL (synchronous)
     */
    @Post(uri = "/convert/url")
    @Operation(
        summary = "Convert document from URL (sync)",
        description = "Provide the document URL as a request parameter (?url=...). Returns converted representations (json, md, text)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conversion completed"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid url"),
        @ApiResponse(responseCode = "500", description = "Conversion failed")
    })
    public HttpResponse<DoclingConversionResponse> convertFromUrl(@QueryValue("url") String url) {
        if (url == null || url.trim().isEmpty()) {
            return HttpResponse.badRequest();
        }

        try {
            DoclingConversionResponse response = doclingClient.convertFromUrl(url);
            return HttpResponse.ok(response);
        } catch (Exception e) {
            logger.error("Failed to convert document from URL: {}", url, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Convert uploaded file (synchronous)
     */
    @Post(uri = "/convert/file", consumes = MediaType.MULTIPART_FORM_DATA)
    @Operation(
        summary = "Convert uploaded file (sync)",
        description = "Upload a document as multipart form-data with field 'file'. The response contains converted representations (json, md, text)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conversion completed"),
        @ApiResponse(responseCode = "400", description = "File missing"),
        @ApiResponse(responseCode = "500", description = "Conversion failed")
    })
    public HttpResponse<DoclingConversionResponse> convertFromFile(@Part("file") CompletedFileUpload file) {
        if (file == null || file.getSize() <= 0) {
            return HttpResponse.badRequest();
        }
        
        try {
            DoclingConversionResponse response = doclingClient.convertFromFile(file);
            return HttpResponse.ok(response);
        } catch (IOException e) {
            logger.error("Failed to convert uploaded file: {}", file.getFilename(), e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Health check for Docling service
     */
    @Get(uri = "/health")
    @Operation(summary = "Docling service health", description = "Returns the health status of the configured Docling Serve instance")
    public HttpResponse<Map<String, Object>> health() {
        boolean isHealthy = doclingClient.isHealthy();
        Map<String, Object> status = Map.of(
            "status", isHealthy ? "UP" : "DOWN",
            "service", "docling-serve"
        );
        
        HttpStatus httpStatus = isHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return HttpResponse.status(httpStatus).body(status);
    }

    // ===================== Chunking Endpoints =====================

    @Post(uri = "/chunk/hybrid/url")
    @Operation(
        summary = "Chunk a document from URL using Hybrid chunker",
        description = "Chunk a remote document. All inputs are request parameters for easy testing via Swagger UI."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chunking completed"),
        @ApiResponse(responseCode = "400", description = "Missing url or invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Chunking failed")
    })
    public HttpResponse<DoclingChunkResponse> hybridChunkFromUrl(
        @Parameter(description = "Public URL of the document to chunk", example = "https://example.com/file.pdf") @QueryValue("url") String url,
        @Parameter(description = "Use markdown table format instead of triplets for tables (default: false)") @QueryValue(value = "use_markdown_tables") Boolean useMarkdownTables,
        @Parameter(description = "Include raw_text field along with contextualized text (default: false)") @QueryValue(value = "include_raw_text") Boolean includeRawText,
        @Parameter(description = "Max tokens per chunk; if omitted, inferred from tokenizer") @QueryValue(value = "max_tokens") Integer maxTokens,
        @Parameter(description = "HuggingFace tokenizer id to estimate token lengths", example = "sentence-transformers/all-MiniLM-L6-v2") @QueryValue(value = "tokenizer") String tokenizer,
        @Parameter(description = "Merge undersized successive chunks with same headings (default: true)") @QueryValue(value = "merge_peers") Boolean mergePeers,
        @Parameter(description = "Include converted document content in the response (inbody)") @QueryValue(value = "include_converted_doc") Boolean includeConvertedDoc,
        @Parameter(description = "Target output; for JSON responses, only 'inbody' is supported") @QueryValue(value = "target_kind") String targetKind,
        // convert options overrides (optional)
        @Parameter(description = "Override Docling convert to_formats (repeat param)") @QueryValue(value = "to_formats") java.util.List<String> toFormats,
        @Parameter(description = "Enable OCR during conversion (default: true)") @QueryValue(value = "do_ocr") Boolean doOcr,
        @Parameter(description = "Enable table structure extraction (default: true)") @QueryValue(value = "do_table_structure") Boolean doTableStructure,
        @Parameter(description = "Table mode: fast or accurate", example = "fast") @QueryValue(value = "table_mode") String tableMode,
        @Parameter(description = "Pipeline: standard | vlm | asr", example = "standard") @QueryValue(value = "pipeline") String pipeline
    ) {
        if (url == null || url.isBlank()) return HttpResponse.badRequest();

        DoclingChunkRequest.HybridChunkerOptions opts = new DoclingChunkRequest.HybridChunkerOptions();
        opts.setUseMarkdownTables(useMarkdownTables);
        opts.setIncludeRawText(includeRawText);
        opts.setMaxTokens(maxTokens);
        opts.setTokenizer(tokenizer);
        opts.setMergePeers(mergePeers);

        if (targetKind != null && !"inbody".equalsIgnoreCase(targetKind)) {
            logger.warn("target_kind='{}' is not supported for JSON response; defaulting to 'inbody'", targetKind);
            targetKind = "inbody";
        }

        Options convertOptions = null;
        if ((toFormats != null && !toFormats.isEmpty()) || doOcr != null || doTableStructure != null || tableMode != null || pipeline != null) {
            convertOptions = new Options();
            if (toFormats != null && !toFormats.isEmpty()) convertOptions.setToFormats(toFormats);
            if (doOcr != null) convertOptions.setDoOcr(doOcr);
            if (doTableStructure != null) convertOptions.setDoTableStructure(doTableStructure);
            if (tableMode != null) convertOptions.setTableMode(tableMode);
            if (pipeline != null) convertOptions.setPipeline(pipeline);
        }

        try {
            DoclingChunkResponse resp = doclingClient.hybridChunkFromUrl(url, opts, includeConvertedDoc, targetKind, convertOptions);
            return HttpResponse.ok(resp);
        } catch (Exception e) {
            logger.error("Hybrid chunking from URL failed: {}", url, e);
            return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Post(uri = "/chunk/hybrid/file", consumes = MediaType.MULTIPART_FORM_DATA)
    @Operation(
        summary = "Chunk an uploaded file using Hybrid chunker",
        description = "Upload a document as multipart form-data (field 'file'). Additional chunking options are form fields."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chunking completed"),
        @ApiResponse(responseCode = "400", description = "Missing file or invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Chunking failed")
    })
    public HttpResponse<DoclingChunkResponse> hybridChunkFromFile(
        @Parameter(description = "File to upload", content = @Content(schema = @Schema(type = "string", format = "binary"))) @Part("file") CompletedFileUpload file,
        @Parameter(description = "Use markdown tables (default: false)") @QueryValue(value = "use_markdown_tables") Boolean useMarkdownTables,
        @Parameter(description = "Include raw_text (default: false)") @QueryValue(value = "include_raw_text") Boolean includeRawText,
        @Parameter(description = "Max tokens per chunk") @QueryValue(value = "max_tokens") Integer maxTokens,
        @Parameter(description = "HF tokenizer id") @QueryValue(value = "tokenizer") String tokenizer,
        @Parameter(description = "Merge undersized peers (default: true)") @QueryValue(value = "merge_peers") Boolean mergePeers,
        @Parameter(description = "Include converted document in response") @QueryValue(value = "include_converted_doc") Boolean includeConvertedDoc,
        @Parameter(description = "Target output; only 'inbody' for JSON response") @QueryValue(value = "target_kind") String targetKind
    ) {
        if (file == null || file.getSize() <= 0) return HttpResponse.badRequest();
        if (targetKind != null && !"inbody".equalsIgnoreCase(targetKind)) {
            logger.warn("target_kind='{}' is not supported for JSON response; defaulting to 'inbody'", targetKind);
            targetKind = "inbody";
        }
        DoclingChunkRequest.HybridChunkerOptions opts = new DoclingChunkRequest.HybridChunkerOptions();
        opts.setUseMarkdownTables(useMarkdownTables);
        opts.setIncludeRawText(includeRawText);
        opts.setMaxTokens(maxTokens);
        opts.setTokenizer(tokenizer);
        opts.setMergePeers(mergePeers);
        try {
            DoclingChunkResponse resp = doclingClient.hybridChunkFromFile(file, opts, includeConvertedDoc, targetKind, null);
            return HttpResponse.ok(resp);
        } catch (Exception e) {
            logger.error("Hybrid chunking from file failed: {}", file.getFilename(), e);
            return HttpResponse.serverError();
        }
    }

    @Post(uri = "/chunk/hierarchical/url")
    @Operation(
        summary = "Chunk a document from URL using Hierarchical chunker",
        description = "Chunk a remote document with section awareness. All inputs are request parameters."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chunking completed"),
        @ApiResponse(responseCode = "400", description = "Missing url or invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Chunking failed")
    })
    public HttpResponse<DoclingChunkResponse> hierarchicalChunkFromUrl(
        @Parameter(description = "Public URL of the document to chunk", example = "https://example.com/file.pdf") @QueryValue("url") String url,
        @Parameter(description = "Use markdown tables (default: false)") @QueryValue(value = "use_markdown_tables") Boolean useMarkdownTables,
        @Parameter(description = "Include raw_text (default: false)") @QueryValue(value = "include_raw_text") Boolean includeRawText,
        @Parameter(description = "Include converted document in response") @QueryValue(value = "include_converted_doc") Boolean includeConvertedDoc,
        @Parameter(description = "Target output; only 'inbody' for JSON response") @QueryValue(value = "target_kind") String targetKind,
        // convert options overrides (optional)
        @Parameter(description = "Override Docling convert to_formats (repeat param)") @QueryValue(value = "to_formats") java.util.List<String> toFormats,
        @Parameter(description = "Enable OCR during conversion (default: true)") @QueryValue(value = "do_ocr") Boolean doOcr,
        @Parameter(description = "Enable table structure extraction (default: true)") @QueryValue(value = "do_table_structure") Boolean doTableStructure,
        @Parameter(description = "Table mode: fast or accurate") @QueryValue(value = "table_mode") String tableMode,
        @Parameter(description = "Pipeline: standard | vlm | asr") @QueryValue(value = "pipeline") String pipeline
    ) {
        if (url == null || url.isBlank()) return HttpResponse.badRequest();

        DoclingChunkRequest.HierarchicalChunkerOptions opts = new DoclingChunkRequest.HierarchicalChunkerOptions();
        opts.setUseMarkdownTables(useMarkdownTables);
        opts.setIncludeRawText(includeRawText);

        if (targetKind != null && !"inbody".equalsIgnoreCase(targetKind)) {
            logger.warn("target_kind='{}' is not supported for JSON response; defaulting to 'inbody'", targetKind);
            targetKind = "inbody";
        }

        Options convertOptions = null;
        if ((toFormats != null && !toFormats.isEmpty()) || doOcr != null || doTableStructure != null || tableMode != null || pipeline != null) {
            convertOptions = new Options();
            if (toFormats != null && !toFormats.isEmpty()) convertOptions.setToFormats(toFormats);
            if (doOcr != null) convertOptions.setDoOcr(doOcr);
            if (doTableStructure != null) convertOptions.setDoTableStructure(doTableStructure);
            if (tableMode != null) convertOptions.setTableMode(tableMode);
            if (pipeline != null) convertOptions.setPipeline(pipeline);
        }

        try {
            DoclingChunkResponse resp = doclingClient.hierarchicalChunkFromUrl(url, opts, includeConvertedDoc, targetKind, convertOptions);
            return HttpResponse.ok(resp);
        } catch (Exception e) {
            logger.error("Hierarchical chunking from URL failed: {}", url, e);
            return HttpResponse.serverError();
        }
    }

    @Post(uri = "/chunk/hierarchical/file", consumes = MediaType.MULTIPART_FORM_DATA)
    @Operation(
        summary = "Chunk an uploaded file using Hierarchical chunker",
        description = "Upload a document as multipart form-data (field 'file'). Additional chunking options are form fields."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chunking completed"),
        @ApiResponse(responseCode = "400", description = "Missing file or invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Chunking failed")
    })
    public HttpResponse<DoclingChunkResponse> hierarchicalChunkFromFile(
        @Parameter(description = "File to upload", content = @Content(schema = @Schema(type = "string", format = "binary"))) @Part("file") CompletedFileUpload file,
        @Parameter(description = "Use markdown tables (default: false)") @QueryValue(value = "use_markdown_tables") Boolean useMarkdownTables,
        @Parameter(description = "Include raw_text (default: false)") @QueryValue(value = "include_raw_text") Boolean includeRawText,
        @Parameter(description = "Include converted document in response") @QueryValue(value = "include_converted_doc") Boolean includeConvertedDoc,
        @Parameter(description = "Target output; only 'inbody' for JSON response") @QueryValue(value = "target_kind") String targetKind
    ) {
        if (file == null || file.getSize() <= 0) return HttpResponse.badRequest();
        if (targetKind != null && !"inbody".equalsIgnoreCase(targetKind)) {
            logger.warn("target_kind='{}' is not supported for JSON response; defaulting to 'inbody'", targetKind);
            targetKind = "inbody";
        }
        DoclingChunkRequest.HierarchicalChunkerOptions opts = new DoclingChunkRequest.HierarchicalChunkerOptions();
        opts.setUseMarkdownTables(useMarkdownTables);
        opts.setIncludeRawText(includeRawText);
        try {
            DoclingChunkResponse resp = doclingClient.hierarchicalChunkFromFile(file, opts, includeConvertedDoc, targetKind, null);
            return HttpResponse.ok(resp);
        } catch (Exception e) {
            logger.error("Hierarchical chunking from file failed: {}", file.getFilename(), e);
            return HttpResponse.serverError();
        }
    }
}
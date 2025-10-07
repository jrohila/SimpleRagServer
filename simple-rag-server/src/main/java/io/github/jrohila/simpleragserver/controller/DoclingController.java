package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.client.DoclingClient;
import io.github.jrohila.simpleragserver.dto.DoclingConversionResponse;
import io.github.jrohila.simpleragserver.dto.DoclingChunkRequest;
import io.github.jrohila.simpleragserver.dto.DoclingChunkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
 

@RestController
@RequestMapping("/api/docling")
@Tag(name = "Docling", description = "Endpoints to convert and chunk documents using Docling Serve")
public class DoclingController {
    
    private static final Logger logger = LoggerFactory.getLogger(DoclingController.class);
    
    private final DoclingClient doclingClient;
    
    @Autowired
    public DoclingController(DoclingClient doclingClient) {
        this.doclingClient = doclingClient;
    }
    
    /**
     * Convert document from URL (synchronous)
     */
    @PostMapping("/convert/url")
    @Operation(
        summary = "Convert document from URL (sync)",
        description = "Provide the document URL as a request parameter (?url=...). Returns converted representations (json, md, text)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conversion completed"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid url"),
        @ApiResponse(responseCode = "500", description = "Conversion failed")
    })
    public ResponseEntity<DoclingConversionResponse> convertFromUrl(@RequestParam("url") String url) {
        if (url == null || url.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            DoclingConversionResponse response = doclingClient.convertFromUrl(url);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to convert document from URL: {}", url, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Convert uploaded file (synchronous)
     */
    @PostMapping(value = "/convert/file", consumes = "multipart/form-data")
    @Operation(
        summary = "Convert uploaded file (sync)",
        description = "Upload a document as multipart form-data with field 'file'. The response contains converted representations (json, md, text)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conversion completed"),
        @ApiResponse(responseCode = "400", description = "File missing"),
        @ApiResponse(responseCode = "500", description = "Conversion failed")
    })
    public ResponseEntity<DoclingConversionResponse> convertFromFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            DoclingConversionResponse response = doclingClient.convertFromFile(file);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Failed to convert uploaded file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Health check for Docling service
     */
    @GetMapping("/health")
    @Operation(summary = "Docling service health", description = "Returns the health status of the configured Docling Serve instance")
    public ResponseEntity<Map<String, Object>> health() {
        boolean isHealthy = doclingClient.isHealthy();
        Map<String, Object> status = Map.of(
            "status", isHealthy ? "UP" : "DOWN",
            "service", "docling-serve"
        );
        
        HttpStatus httpStatus = isHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(status);
    }

    // ===================== Chunking Endpoints =====================

    @PostMapping("/chunk/hybrid/url")
    @Operation(
        summary = "Chunk a document from URL using Hybrid chunker",
        description = "Chunk a remote document. All inputs are request parameters for easy testing via Swagger UI."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chunking completed"),
        @ApiResponse(responseCode = "400", description = "Missing url or invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Chunking failed")
    })
    public ResponseEntity<DoclingChunkResponse> hybridChunkFromUrl(
        @Parameter(description = "Public URL of the document to chunk", example = "https://example.com/file.pdf") @RequestParam("url") String url,
        @Parameter(description = "Use markdown table format instead of triplets for tables (default: false)") @RequestParam(value = "use_markdown_tables", required = false) Boolean useMarkdownTables,
        @Parameter(description = "Include raw_text field along with contextualized text (default: false)") @RequestParam(value = "include_raw_text", required = false) Boolean includeRawText,
        @Parameter(description = "Max tokens per chunk; if omitted, inferred from tokenizer") @RequestParam(value = "max_tokens", required = false) Integer maxTokens,
        @Parameter(description = "HuggingFace tokenizer id to estimate token lengths", example = "sentence-transformers/all-MiniLM-L6-v2") @RequestParam(value = "tokenizer", required = false) String tokenizer,
        @Parameter(description = "Merge undersized successive chunks with same headings (default: true)") @RequestParam(value = "merge_peers", required = false) Boolean mergePeers,
        @Parameter(description = "Include converted document content in the response (inbody)") @RequestParam(value = "include_converted_doc", required = false) Boolean includeConvertedDoc,
        @Parameter(description = "Target output; for JSON responses, only 'inbody' is supported") @RequestParam(value = "target_kind", required = false) String targetKind,
        // convert options overrides (optional)
        @Parameter(description = "Override Docling convert to_formats (repeat param)") @RequestParam(value = "to_formats", required = false) java.util.List<String> toFormats,
        @Parameter(description = "Enable OCR during conversion (default: true)") @RequestParam(value = "do_ocr", required = false) Boolean doOcr,
        @Parameter(description = "Enable table structure extraction (default: true)") @RequestParam(value = "do_table_structure", required = false) Boolean doTableStructure,
        @Parameter(description = "Table mode: fast or accurate", example = "fast") @RequestParam(value = "table_mode", required = false) String tableMode,
        @Parameter(description = "Pipeline: standard | vlm | asr", example = "standard") @RequestParam(value = "pipeline", required = false) String pipeline
    ) {
        if (url == null || url.isBlank()) return ResponseEntity.badRequest().build();

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

        io.github.jrohila.simpleragserver.dto.DoclingConversionRequest.Options convertOptions = null;
        if ((toFormats != null && !toFormats.isEmpty()) || doOcr != null || doTableStructure != null || tableMode != null || pipeline != null) {
            convertOptions = new io.github.jrohila.simpleragserver.dto.DoclingConversionRequest.Options();
            if (toFormats != null && !toFormats.isEmpty()) convertOptions.setToFormats(toFormats);
            if (doOcr != null) convertOptions.setDoOcr(doOcr);
            if (doTableStructure != null) convertOptions.setDoTableStructure(doTableStructure);
            if (tableMode != null) convertOptions.setTableMode(tableMode);
            if (pipeline != null) convertOptions.setPipeline(pipeline);
        }

        try {
            DoclingChunkResponse resp = doclingClient.hybridChunkFromUrl(url, opts, includeConvertedDoc, targetKind, convertOptions);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Hybrid chunking from URL failed: {}", url, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/chunk/hybrid/file", consumes = "multipart/form-data")
    @Operation(
        summary = "Chunk an uploaded file using Hybrid chunker",
        description = "Upload a document as multipart form-data (field 'file'). Additional chunking options are form fields."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chunking completed"),
        @ApiResponse(responseCode = "400", description = "Missing file or invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Chunking failed")
    })
    public ResponseEntity<DoclingChunkResponse> hybridChunkFromFile(
        @Parameter(description = "File to upload", content = @Content(schema = @Schema(type = "string", format = "binary"))) @RequestPart("file") MultipartFile file,
        @Parameter(description = "Use markdown tables (default: false)") @RequestParam(value = "use_markdown_tables", required = false) Boolean useMarkdownTables,
        @Parameter(description = "Include raw_text (default: false)") @RequestParam(value = "include_raw_text", required = false) Boolean includeRawText,
        @Parameter(description = "Max tokens per chunk") @RequestParam(value = "max_tokens", required = false) Integer maxTokens,
        @Parameter(description = "HF tokenizer id") @RequestParam(value = "tokenizer", required = false) String tokenizer,
        @Parameter(description = "Merge undersized peers (default: true)") @RequestParam(value = "merge_peers", required = false) Boolean mergePeers,
        @Parameter(description = "Include converted document in response") @RequestParam(value = "include_converted_doc", required = false) Boolean includeConvertedDoc,
        @Parameter(description = "Target output; only 'inbody' for JSON response") @RequestParam(value = "target_kind", required = false) String targetKind
    ) {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().build();
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
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Hybrid chunking from file failed: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/chunk/hierarchical/url")
    @Operation(
        summary = "Chunk a document from URL using Hierarchical chunker",
        description = "Chunk a remote document with section awareness. All inputs are request parameters."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chunking completed"),
        @ApiResponse(responseCode = "400", description = "Missing url or invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Chunking failed")
    })
    public ResponseEntity<DoclingChunkResponse> hierarchicalChunkFromUrl(
        @Parameter(description = "Public URL of the document to chunk", example = "https://example.com/file.pdf") @RequestParam("url") String url,
        @Parameter(description = "Use markdown tables (default: false)") @RequestParam(value = "use_markdown_tables", required = false) Boolean useMarkdownTables,
        @Parameter(description = "Include raw_text (default: false)") @RequestParam(value = "include_raw_text", required = false) Boolean includeRawText,
        @Parameter(description = "Include converted document in response") @RequestParam(value = "include_converted_doc", required = false) Boolean includeConvertedDoc,
        @Parameter(description = "Target output; only 'inbody' for JSON response") @RequestParam(value = "target_kind", required = false) String targetKind,
        // convert options overrides (optional)
        @Parameter(description = "Override Docling convert to_formats (repeat param)") @RequestParam(value = "to_formats", required = false) java.util.List<String> toFormats,
        @Parameter(description = "Enable OCR during conversion (default: true)") @RequestParam(value = "do_ocr", required = false) Boolean doOcr,
        @Parameter(description = "Enable table structure extraction (default: true)") @RequestParam(value = "do_table_structure", required = false) Boolean doTableStructure,
        @Parameter(description = "Table mode: fast or accurate") @RequestParam(value = "table_mode", required = false) String tableMode,
        @Parameter(description = "Pipeline: standard | vlm | asr") @RequestParam(value = "pipeline", required = false) String pipeline
    ) {
        if (url == null || url.isBlank()) return ResponseEntity.badRequest().build();

        DoclingChunkRequest.HierarchicalChunkerOptions opts = new DoclingChunkRequest.HierarchicalChunkerOptions();
        opts.setUseMarkdownTables(useMarkdownTables);
        opts.setIncludeRawText(includeRawText);

        if (targetKind != null && !"inbody".equalsIgnoreCase(targetKind)) {
            logger.warn("target_kind='{}' is not supported for JSON response; defaulting to 'inbody'", targetKind);
            targetKind = "inbody";
        }

        io.github.jrohila.simpleragserver.dto.DoclingConversionRequest.Options convertOptions = null;
        if ((toFormats != null && !toFormats.isEmpty()) || doOcr != null || doTableStructure != null || tableMode != null || pipeline != null) {
            convertOptions = new io.github.jrohila.simpleragserver.dto.DoclingConversionRequest.Options();
            if (toFormats != null && !toFormats.isEmpty()) convertOptions.setToFormats(toFormats);
            if (doOcr != null) convertOptions.setDoOcr(doOcr);
            if (doTableStructure != null) convertOptions.setDoTableStructure(doTableStructure);
            if (tableMode != null) convertOptions.setTableMode(tableMode);
            if (pipeline != null) convertOptions.setPipeline(pipeline);
        }

        try {
            DoclingChunkResponse resp = doclingClient.hierarchicalChunkFromUrl(url, opts, includeConvertedDoc, targetKind, convertOptions);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Hierarchical chunking from URL failed: {}", url, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/chunk/hierarchical/file", consumes = "multipart/form-data")
    @Operation(
        summary = "Chunk an uploaded file using Hierarchical chunker",
        description = "Upload a document as multipart form-data (field 'file'). Additional chunking options are form fields."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chunking completed"),
        @ApiResponse(responseCode = "400", description = "Missing file or invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Chunking failed")
    })
    public ResponseEntity<DoclingChunkResponse> hierarchicalChunkFromFile(
        @Parameter(description = "File to upload", content = @Content(schema = @Schema(type = "string", format = "binary"))) @RequestPart("file") MultipartFile file,
        @Parameter(description = "Use markdown tables (default: false)") @RequestParam(value = "use_markdown_tables", required = false) Boolean useMarkdownTables,
        @Parameter(description = "Include raw_text (default: false)") @RequestParam(value = "include_raw_text", required = false) Boolean includeRawText,
        @Parameter(description = "Include converted document in response") @RequestParam(value = "include_converted_doc", required = false) Boolean includeConvertedDoc,
        @Parameter(description = "Target output; only 'inbody' for JSON response") @RequestParam(value = "target_kind", required = false) String targetKind
    ) {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().build();
        if (targetKind != null && !"inbody".equalsIgnoreCase(targetKind)) {
            logger.warn("target_kind='{}' is not supported for JSON response; defaulting to 'inbody'", targetKind);
            targetKind = "inbody";
        }
        DoclingChunkRequest.HierarchicalChunkerOptions opts = new DoclingChunkRequest.HierarchicalChunkerOptions();
        opts.setUseMarkdownTables(useMarkdownTables);
        opts.setIncludeRawText(includeRawText);
        try {
            DoclingChunkResponse resp = doclingClient.hierarchicalChunkFromFile(file, opts, includeConvertedDoc, targetKind, null);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Hierarchical chunking from file failed: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
package io.github.jrohila.simpleragserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoclingConversionRequest {

    // Matches OpenAPI: ConvertDocumentsRequest
    @JsonProperty("options")
    private Options options = Options.defaultForRag();

    @JsonProperty("sources")
    private List<SourceInput> sources;

    // Optional: target (defaults to inbody on server). Not required to set.
    // @JsonProperty("target")
    // private Target target;

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Options {
        @JsonProperty("to_formats")
        private List<String> toFormats; // e.g. ["json","md"]

        @JsonProperty("from_formats")
        private List<String> fromFormats;

        @JsonProperty("image_export_mode")
        private String imageExportMode; // embedded|placeholder|referenced

        @JsonProperty("do_ocr")
        private Boolean doOcr;

        @JsonProperty("force_ocr")
        private Boolean forceOcr;

        @JsonProperty("ocr_engine")
        private String ocrEngine;

        @JsonProperty("pdf_backend")
        private String pdfBackend;

        @JsonProperty("table_mode")
        private String tableMode; // fast|accurate

        @JsonProperty("table_cell_matching")
        private Boolean tableCellMatching;

        @JsonProperty("pipeline")
        private String pipeline; // standard|vlm|asr

        @JsonProperty("page_range")
        private List<Integer> pageRange;

        @JsonProperty("document_timeout")
        private Double documentTimeout;

        @JsonProperty("abort_on_error")
        private Boolean abortOnError;

        @JsonProperty("do_table_structure")
        private Boolean doTableStructure;

        @JsonProperty("include_images")
        private Boolean includeImages;

        @JsonProperty("images_scale")
        private Double imagesScale;

        @JsonProperty("md_page_break_placeholder")
        private String mdPageBreakPlaceholder;

        public static Options defaultForRag() {
            Options o = new Options();
            o.setToFormats(List.of("json", "md"));
            o.setDoOcr(true);
            o.setPipeline("standard");
            o.setDoTableStructure(true);
            o.setTableMode("fast");
            o.setAbortOnError(false);
            // leave other fields to server defaults
            return o;
        }
    }

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SourceInput {
        @JsonProperty("kind")
        private String kind; // "http" | "file" | "s3"

        // For kind=http
        @JsonProperty("url")
        private String url;
        @JsonProperty("headers")
        private Map<String, Object> headers;

        // For kind=file
        @JsonProperty("filename")
        private String filename;
        @JsonProperty("base64_string")
        private String base64String;
    }

    // Factory methods for convenience
    public static DoclingConversionRequest fromUrl(String url) {
        DoclingConversionRequest request = new DoclingConversionRequest();
        SourceInput source = new SourceInput();
        source.setKind("http");
        source.setUrl(url);
        request.setSources(List.of(source));
        return request;
    }

    public static DoclingConversionRequest fromBase64(String filename, String base64Content) {
        DoclingConversionRequest request = new DoclingConversionRequest();
        SourceInput source = new SourceInput();
        source.setKind("file");
        source.setFilename(filename);
        source.setBase64String(base64Content);
        request.setSources(List.of(source));
        return request;
    }
}
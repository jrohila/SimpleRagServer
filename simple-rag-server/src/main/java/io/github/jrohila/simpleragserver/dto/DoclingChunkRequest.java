package io.github.jrohila.simpleragserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoclingChunkRequest {

    @JsonProperty("convert_options")
    private DoclingConversionRequest.Options convertOptions = DoclingConversionRequest.Options.defaultForRag();

    @JsonProperty("sources")
    private List<DoclingConversionRequest.SourceInput> sources;

    @JsonProperty("include_converted_doc")
    private Boolean includeConvertedDoc = false;

    @JsonProperty("target")
    private Target target; // optional, defaults to inbody on server side

    @JsonProperty("chunking_options")
    private Object chunkingOptions; // HybridChunkerOptions or HierarchicalChunkerOptions depending on endpoint

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Target {
        @JsonProperty("kind")
        private String kind; // inbody | zip | s3 | put
    }

    // Options for /v1/chunk/hybrid endpoints
    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HybridChunkerOptions {
        @JsonProperty("use_markdown_tables")
        private Boolean useMarkdownTables;

        @JsonProperty("include_raw_text")
        private Boolean includeRawText;

        @JsonProperty("max_tokens")
        private Integer maxTokens;

        @JsonProperty("tokenizer")
        private String tokenizer;

        @JsonProperty("merge_peers")
        private Boolean mergePeers;
    }

    // Options for /v1/chunk/hierarchical endpoints
    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HierarchicalChunkerOptions {
        @JsonProperty("use_markdown_tables")
        private Boolean useMarkdownTables;

        @JsonProperty("include_raw_text")
        private Boolean includeRawText;
    }
}

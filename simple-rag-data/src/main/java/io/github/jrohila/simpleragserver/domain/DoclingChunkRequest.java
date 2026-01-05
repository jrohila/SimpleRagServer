package io.github.jrohila.simpleragserver.domain;

// Jackson annotations removed for SimpleRagData module
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class DoclingChunkRequest {

    private Options convertOptions = Options.defaultForRag();
    private List<SourceInput> sources;
    private Boolean includeConvertedDoc = false;
    private Target target; // optional, defaults to inbody on server side
    private Object chunkingOptions; // HybridChunkerOptions or HierarchicalChunkerOptions depending on endpoint

    @Getter
    @Setter
    @ToString
    public static class Target {
        private String kind; // inbody | zip | s3 | put
    }

    // Options for /v1/chunk/hybrid endpoints
    @Getter
    @Setter
    @ToString
    public static class HybridChunkerOptions {
        private Boolean useMarkdownTables;
        private Boolean includeRawText;
        private Integer maxTokens;
        private String tokenizer;
        private Boolean mergePeers;
    }

    // Options for /v1/chunk/hierarchical endpoints
    @Getter
    @Setter
    @ToString
    public static class HierarchicalChunkerOptions {
        private Boolean useMarkdownTables;
        private Boolean includeRawText;
    }
}

package io.github.jrohila.simpleragserver.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class DoclingChunkResponse {

    @JsonProperty("chunks")
    private List<Chunk> chunks;

    @JsonProperty("converted")
    private Converted converted; // present when include_converted_doc=true

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Chunk {
        @JsonProperty("filename")
        private String filename;
        
        @JsonProperty("chunk_index")
        private Integer chunkIndex;
        
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("raw_text")
        private String rawText;
        
        @JsonProperty("num_tokens")
        private Integer numTokens;
        
        @JsonProperty("headings")
        private List<String> headings;
        
        @JsonProperty("captions")
        private List<String> captions;
        
        @JsonProperty("doc_items")
        private List<String> docItems;
        
        @JsonProperty("page_numbers")
        private List<Integer> pageNumbers;
        
        @JsonProperty("metadata")
        private Metadata metadata;
        
        // Convenience methods for backward compatibility
        public String getTitle() {
            if (headings != null && !headings.isEmpty()) {
                return headings.get(0);
            }
            return null;
        }
        
        public Integer getPageNumber() {
            if (pageNumbers != null && !pageNumbers.isEmpty()) {
                return pageNumbers.get(0);
            }
            return null;
        }
    }

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        @JsonProperty("origin")
        private Origin origin;
    }

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Origin {
        @JsonProperty("mimetype")
        private String mimetype;
        
        @JsonProperty("binary_hash")
        private String binaryHash;
        
        @JsonProperty("filename")
        private String filename;
        
        @JsonProperty("uri")
        private String uri;
    }

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Converted {
        @JsonProperty("files")
        private Map<String, String> files; // format -> content (json, md, text), when target=inbody
        @JsonProperty("metadata")
        private Map<String, Object> metadata;
    }
}

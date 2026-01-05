package io.github.jrohila.simpleragserver.domain;

// Jackson annotations removed for SimpleRagData module
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class DoclingChunkResponse {

    private List<Chunk> chunks;
    private Converted converted; // present when include_converted_doc=true

    @Getter
    @Setter
    @ToString
    public static class Chunk {
        private String filename;
        private Integer chunkIndex;
        private String text;
        private String rawText;
        private Integer numTokens;
        private List<String> headings;
        private List<String> captions;
        private List<String> docItems;
        private List<Integer> pageNumbers;
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
    public static class Metadata {
        private Origin origin;
    }

    @Getter
    @Setter
    @ToString
    public static class Origin {
        private String mimetype;
        private String binaryHash;
        private String filename;
        private String uri;
    }

    @Getter
    @Setter
    @ToString
    public static class Converted {
        private Map<String, String> files; // format -> content (json, md, text), when target=inbody
        private Map<String, Object> metadata;
    }
}

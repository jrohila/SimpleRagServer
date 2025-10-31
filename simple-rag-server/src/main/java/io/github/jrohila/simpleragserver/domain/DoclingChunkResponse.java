package io.github.jrohila.simpleragserver.domain;

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
public class DoclingChunkResponse {

    @JsonProperty("chunks")
    private List<Chunk> chunks;

    @JsonProperty("converted")
    private Converted converted; // present when include_converted_doc=true

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Chunk {
        @JsonProperty("id")
        private String id;
        @JsonProperty("text")
        private String text;
        @JsonProperty("title")
        private String title;
        @JsonProperty("section")
        private String section;
        @JsonProperty("page_number")
        private Integer pageNumber;
        @JsonProperty("metadata")
        private Map<String, Object> metadata;
    }

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Converted {
        @JsonProperty("files")
        private Map<String, String> files; // format -> content (json, md, text), when target=inbody
        @JsonProperty("metadata")
        private Map<String, Object> metadata;
    }
}

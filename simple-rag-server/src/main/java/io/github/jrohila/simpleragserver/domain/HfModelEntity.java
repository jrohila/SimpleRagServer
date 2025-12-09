package io.github.jrohila.simpleragserver.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Representation of a Hugging Face model stored in OpenSearch.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HfModelEntity {

    private String id;
    private String author;
    private List<String> tags;
    private List<String> pipelineTags;

    /** Files with filename and resolved URL */
    private List<HfModelFile> files;

    private long totalWeightBytes;
    private double totalWeightMB;
    private boolean hasOnnx;

    // Optional metadata from the models-json endpoint
    private Integer downloads;
    private Integer likes;
    private String lastModified;
    private String repoType;
    private Boolean gated;
    private Boolean privateRepo;

    private List<String> availableInferenceProviders;

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HfModelFile {
        private String filename;
        private String url;
    }
}

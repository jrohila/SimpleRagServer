package io.github.jrohila.simpleragserver.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentEntity {

    public enum ProcessingState {
        OPEN,
        PROCESSING,
        DONE,
        FAILED
    }

    private String id;

    private ProcessingState state;

    private String originalFilename;

    private String contentId;

    private Long contentLen;

    private String mimeType;
    
    private String hash;

    private String operationId; // Docling ID to check processing status
    
    private String createdTime;

    private String updatedTime;
}

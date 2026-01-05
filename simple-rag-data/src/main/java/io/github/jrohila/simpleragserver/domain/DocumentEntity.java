package io.github.jrohila.simpleragserver.domain;

// Jackson annotations removed for SimpleRagData module
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
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

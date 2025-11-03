package io.github.jrohila.simpleragserver.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

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

    @Id
    private String id;

    private ProcessingState state;

    @OriginalFileName
    private String originalFilename;

    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLen;

    @MimeType
    private String mimeType;
    
    private String hash;

    private String createdTime;

    private String updatedTime;
}

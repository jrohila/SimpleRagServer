package io.github.jrohila.simpleragserver.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

@Getter
@Setter
@ToString
@Document(indexName = "documents")
public class DocumentEntity {

    public enum ProcessingState {
        OPEN,
        PROCESSING,
        DONE,
        FAILED
    }

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private ProcessingState state;

    @OriginalFileName
    @Field(type = FieldType.Text)
    private String originalFilename;

    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLen;

    @MimeType
    private String mimeType;
    
    @Field(type = FieldType.Keyword) 
    private String hash;

    @CreatedDate
    @Field(type = FieldType.Date)
    private Instant createdTime;

    @LastModifiedDate
    @Field(type = FieldType.Date)
    private Instant updatedTime;
}

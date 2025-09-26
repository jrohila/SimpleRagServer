package io.github.jrohila.simpleragserver.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DocumentDto {
    private String id;
    private String fileId;
    private Long createdTime;
    private Long updatedTime;
    private String originalFilename;
    private String fileType;
    private String language;
    private long fileSize;
    private String uploader;
    private String description;
    private java.util.List<String> tags;
    private String checksum;
    private String status;

    public DocumentDto() {} 

    public DocumentDto(String id, String fileId, long createdTime, long updatedTime, String originalFilename, String fileType, String language, long fileSize, String uploader, String description, List<String> tags, String checksum, String status) {
        this.id = id;
        this.fileId = fileId;
        this.createdTime = createdTime;
        this.updatedTime = updatedTime;
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.language = language;
        this.fileSize = fileSize;
        this.uploader = uploader;
        this.description = description;
        this.tags = tags;
        this.checksum = checksum;
        this.status = status;
    }
}
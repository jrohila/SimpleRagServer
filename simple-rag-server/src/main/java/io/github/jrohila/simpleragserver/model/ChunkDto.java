package io.github.jrohila.simpleragserver.model;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ChunkDto {
    private String id;
    private String text;
    private String type;
    private String sectionTitle;
    private int pageNumber;
    private String language;
    private Instant created;
    private Instant modified;
    private String documentId;

    public ChunkDto() {}

    public ChunkDto(String id, String text, String type, String sectionTitle, int pageNumber, String language, Instant created, Instant modified, String documentId) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.sectionTitle = sectionTitle;
        this.pageNumber = pageNumber;
        this.language = language;
        this.created = created;
        this.modified = modified;
        this.documentId = documentId;
    }
}
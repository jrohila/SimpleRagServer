package io.github.jrohila.simpleragserver.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@ToString
@Document(indexName = "chunks", createIndex = false)
public class ChunkEntity {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String text;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Text)
    private String sectionTitle;

    @Field(type = FieldType.Integer)
    private int pageNumber;

    @Field(type = FieldType.Keyword)
    private String language;

    @Field(type = FieldType.Keyword)
    private String hash;

    @Field(type = FieldType.Text)
    private String documentName;

    @CreatedDate
    @Field(type = FieldType.Date)
    private Instant created;

    @LastModifiedDate
    @Field(type = FieldType.Date)
    private Instant modified;

    // Reference to the parent document by id (denormalized reference)
    @Field(type = FieldType.Keyword)
    private String documentId;

    // Embedding stored for KNN search - ensure index mapping defines knn_vector
    // Spring Data doesn't have a dedicated annotation for knn_vector; create index with proper mapping.
    private List<Float> embedding;
}

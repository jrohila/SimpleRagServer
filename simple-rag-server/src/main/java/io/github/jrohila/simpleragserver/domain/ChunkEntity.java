package io.github.jrohila.simpleragserver.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkEntity {

    private String id;

    private String text;

    private String type;

    private String sectionTitle;

    private int pageNumber;

    private String language;

    private String hash;

    private String documentName;

    private String created;

    private String modified;

    // Reference to the parent document by id (denormalized reference)
    private String documentId;

    // Embedding stored for KNN search - ensure index mapping defines knn_vector
    // Spring Data doesn't have a dedicated annotation for knn_vector; create index with proper mapping.
    private List<Float> embedding;
}

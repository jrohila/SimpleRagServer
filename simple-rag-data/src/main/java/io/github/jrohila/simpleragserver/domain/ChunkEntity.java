package io.github.jrohila.simpleragserver.domain;


// Jackson annotations removed for SimpleRagData module
import java.util.List;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }

    @Override
    public String toString() {
        return "ChunkEntity{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", type='" + type + '\'' +
                ", sectionTitle='" + sectionTitle + '\'' +
                ", pageNumber=" + pageNumber +
                ", language='" + language + '\'' +
                ", hash='" + hash + '\'' +
                ", documentName='" + documentName + '\'' +
                ", created='" + created + '\'' +
                ", modified='" + modified + '\'' +
                ", documentId='" + documentId + '\'' +
                ", embedding=" + embedding +
                '}';
    }
}

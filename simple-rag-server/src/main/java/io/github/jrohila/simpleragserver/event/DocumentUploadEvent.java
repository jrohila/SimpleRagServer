package io.github.jrohila.simpleragserver.event;

import lombok.Getter;

/**
 * Event published when a document is uploaded
 */
@Getter
public class DocumentUploadEvent {
    private final String collectionId;
    private final String documentId;

    public DocumentUploadEvent(String collectionId, String documentId) {
        this.collectionId = collectionId;
        this.documentId = documentId;
    }
}

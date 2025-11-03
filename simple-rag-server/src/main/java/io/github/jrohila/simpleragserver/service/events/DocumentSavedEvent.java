package io.github.jrohila.simpleragserver.service.events;

/**
 * Lightweight domain event indicating a document has been saved and should be processed.
 */
public record DocumentSavedEvent(String collectionId, String documentId) {}

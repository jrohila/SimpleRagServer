package io.github.jrohila.simpleragserver.repository;

import org.springframework.ai.document.Document;

import io.github.jrohila.simpleragserver.model.ChunkDto;

public class ChunkMapper {
    public static Document toDocument(ChunkDto c) {
        var metadata = new java.util.HashMap<String, Object>();
        if (c.getType() != null) metadata.put("type", c.getType());
        if (c.getSectionTitle() != null) metadata.put("sectionTitle", c.getSectionTitle());
    if (c.getPageNumber() != 0) metadata.put("pageNumber", c.getPageNumber());
        if (c.getLanguage() != null) metadata.put("language", c.getLanguage());
        if (c.getCreated() != null) metadata.put("created", c.getCreated().toString());
        if (c.getModified() != null) metadata.put("modified", c.getModified().toString());
        if (c.getDocumentId() != null) metadata.put("documentId", c.getDocumentId());
        // Note: vector goes separately via VectorStore API
        var id = c.getId() != null ? c.getId() : java.util.UUID.randomUUID().toString();
        // Ensure non-empty text for embedding
        String text;
        if (c.getText() != null && !c.getText().isBlank()) {
            text = c.getText();
        } else {
            var parts = new java.util.ArrayList<String>();
            if (c.getSectionTitle() != null && !c.getSectionTitle().isBlank()) parts.add(c.getSectionTitle());
            if (c.getType() != null && !c.getType().isBlank()) parts.add(c.getType());
            if (c.getLanguage() != null && !c.getLanguage().isBlank()) parts.add(c.getLanguage());
            if (c.getPageNumber() > 0) parts.add("page:" + c.getPageNumber());
            if (parts.isEmpty()) parts.add("chunk" + (c.getId() != null ? (" " + c.getId()) : ""));
            text = String.join(" ", parts);
        }
        var doc = new Document(id, text, metadata);
        return doc;
    }

    public static ChunkDto fromDocument(Document d) {
        var m = d.getMetadata();

        String id = d.getId();
        String text = d.getText() != null ? d.getText() : "";
        String type = m.get("type") instanceof String s ? s : null;
        String sectionTitle = m.get("sectionTitle") instanceof String s ? s : null;
        Integer pageNumber = m.get("pageNumber") instanceof Number n ? n.intValue() : null;
        String language = m.get("language") instanceof String s ? s : null;
        java.time.Instant created = null;
        if (m.get("created") instanceof String s) {
            try { created = java.time.Instant.parse(s); } catch (Exception ignored) {}
        }
        java.time.Instant modified = null;
        if (m.get("modified") instanceof String s) {
            try { modified = java.time.Instant.parse(s); } catch (Exception ignored) {}
        }
        String documentId = m.get("documentId") instanceof String s ? s : null;

        return new ChunkDto(id, text, type, sectionTitle, pageNumber != null ? pageNumber : 0, language, created, modified, documentId);
    }
}
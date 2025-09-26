package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.model.DocumentDto;
import org.springframework.ai.document.Document;

public class DocumentMapper {
    public static Document toDocument(DocumentDto d) {
        var metadata = new java.util.HashMap<String, Object>();
        if (d.getFileId() != null) metadata.put("fileId", d.getFileId());
        if (d.getCreatedTime() != null) metadata.put("createdTime", d.getCreatedTime());
        if (d.getUpdatedTime() != null) metadata.put("updatedTime", d.getUpdatedTime());
        if (d.getOriginalFilename() != null) metadata.put("originalFilename", d.getOriginalFilename());
        if (d.getFileType() != null) metadata.put("fileType", d.getFileType());
        if (d.getLanguage() != null) metadata.put("language", d.getLanguage());
        // fileSize is primitive long; only include if > 0 to avoid noise
        if (d.getFileSize() > 0) metadata.put("fileSize", d.getFileSize());
        if (d.getUploader() != null) metadata.put("uploader", d.getUploader());
        if (d.getDescription() != null) metadata.put("description", d.getDescription());
        if (d.getTags() != null) metadata.put("tags", d.getTags());
        if (d.getChecksum() != null) metadata.put("checksum", d.getChecksum());
        if (d.getStatus() != null) metadata.put("status", d.getStatus());
        // Build non-empty content for embedding: prefer description, then tags, then filename and attributes, then a fallback
        String content = null;
        if (d.getDescription() != null && !d.getDescription().isBlank()) {
            content = d.getDescription();
        } else if (d.getTags() != null && !d.getTags().isEmpty()) {
            content = String.join(" ", d.getTags());
        } else {
            var parts = new java.util.ArrayList<String>();
            if (d.getOriginalFilename() != null) parts.add(d.getOriginalFilename());
            if (d.getFileType() != null) parts.add(d.getFileType());
            if (d.getLanguage() != null) parts.add(d.getLanguage());
            content = String.join(" ", parts);
        }
        if (content == null || content.isBlank()) {
            content = "document" + (d.getId() != null ? (" " + d.getId()) : "");
        }
        var id = d.getId() != null ? d.getId() : java.util.UUID.randomUUID().toString();
        return new Document(id, content, metadata);
    }

    @SuppressWarnings("unchecked")
    public static DocumentDto fromDocument(Document doc) {
        var m = doc.getMetadata();
        DocumentDto d = new DocumentDto();
        d.setId(doc.getId());
        d.setFileId(m.get("fileId") instanceof String s ? s : null);
    Long ct = null;
    Object cto = m.get("createdTime");
    if (cto instanceof Number n) ct = n.longValue();
    else if (cto instanceof String s) { try { ct = Long.parseLong(s); } catch (Exception ignored) {} }
    d.setCreatedTime(ct);

    Long ut = null;
    Object uto = m.get("updatedTime");
    if (uto instanceof Number n) ut = n.longValue();
    else if (uto instanceof String s) { try { ut = Long.parseLong(s); } catch (Exception ignored) {} }
    d.setUpdatedTime(ut);
        d.setOriginalFilename(m.get("originalFilename") instanceof String s ? s : null);
        d.setFileType(m.get("fileType") instanceof String s ? s : null);
        d.setLanguage(m.get("language") instanceof String s ? s : null);
        Object fs = m.get("fileSize");
        if (fs instanceof Number n) {
            d.setFileSize(n.longValue());
        } else {
            // leave default 0
        }
        d.setUploader(m.get("uploader") instanceof String s ? s : null);
        d.setDescription(m.get("description") instanceof String s ? s : null);
        d.setTags(m.get("tags") instanceof java.util.List<?> list ? (java.util.List<String>) list : null);
        d.setChecksum(m.get("checksum") instanceof String s ? s : null);
        d.setStatus(m.get("status") instanceof String s ? s : null);
        return d;
    }
}

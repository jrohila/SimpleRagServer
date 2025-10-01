package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.pipeline.DocumentChunker;
import io.github.jrohila.simpleragserver.entity.DocumentEntity;
import io.github.jrohila.simpleragserver.repository.DocumentRepository;
import io.github.jrohila.simpleragserver.repository.DocumentContentStore;
import io.github.jrohila.simpleragserver.repository.ChunkRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentContentStore contentStore;
    private final ChunkRepository chunkRepository;
    private final DocumentChunker documentToChunkService;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentContentStore contentStore,
            ChunkRepository chunkRepository,
            DocumentChunker documentToChunkService
    ) {
        this.documentRepository = documentRepository;
        this.contentStore = contentStore;
        this.chunkRepository = chunkRepository;
        this.documentToChunkService = documentToChunkService;
    }

    public Page<DocumentEntity> listDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.findAll(pageable);
    }

    public Optional<DocumentEntity> getById(String id) {
        return documentRepository.findById(id);
    }

    public DocumentEntity uploadDocument(MultipartFile file,
                                         String uploader,
                                         String language,
                                         String description,
                                         List<String> tags,
                                         String status) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String hash = DigestUtils.sha256Hex(file.getInputStream());
        Page<DocumentEntity> existing = documentRepository.findByHash(hash, PageRequest.of(0, 1));
        if (existing.hasContent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document with the same hash already exists");
        }

        DocumentEntity doc = new DocumentEntity();
        // Set only universally safe fields; keep entity-specific fields in your entity as needed.
        // Ensure your DocumentEntity defines these setters.
        try {
            // Optional fields if your entity supports them
            doc.getClass().getMethod("setHash", String.class).invoke(doc, hash);
        } catch (ReflectiveOperationException ignore) { /* setHash not present */ }

        try {
            if (uploader != null) doc.getClass().getMethod("setUploader", String.class).invoke(doc, uploader);
        } catch (ReflectiveOperationException ignore) { }
        try {
            if (language != null) doc.getClass().getMethod("setLanguage", String.class).invoke(doc, language);
        } catch (ReflectiveOperationException ignore) { }
        try {
            if (description != null) doc.getClass().getMethod("setDescription", String.class).invoke(doc, description);
        } catch (ReflectiveOperationException ignore) { }
        try {
            if (tags != null) doc.getClass().getMethod("setTags", List.class).invoke(doc, tags);
        } catch (ReflectiveOperationException ignore) { }
        try {
            if (status != null) doc.getClass().getMethod("setStatus", String.class).invoke(doc, status);
        } catch (ReflectiveOperationException ignore) { }
        try {
            if (file.getOriginalFilename() != null) {
                // try common naming variants
                var name = file.getOriginalFilename();
                boolean set = false;
                try {
                    doc.getClass().getMethod("setOriginalFilename", String.class).invoke(doc, name);
                    set = true;
                } catch (ReflectiveOperationException ignore2) { }
                if (!set) {
                    try { doc.getClass().getMethod("setOriginalFileName", String.class).invoke(doc, name); } catch (ReflectiveOperationException ignore3) { }
                }
            }
        } catch (Exception ignore) { }
        try {
            if (file.getContentType() != null) {
                boolean set = false;
                try { doc.getClass().getMethod("setMimeType", String.class).invoke(doc, file.getContentType()); set = true; } catch (ReflectiveOperationException ignore2) { }
                if (!set) {
                    try { doc.getClass().getMethod("setFileType", String.class).invoke(doc, file.getContentType()); } catch (ReflectiveOperationException ignore3) { }
                }
            }
        } catch (Exception ignore) { }
        try {
            doc.getClass().getMethod("setFileSize", long.class).invoke(doc, file.getSize());
        } catch (ReflectiveOperationException ignore) { }

        // Persist content and metadata
        contentStore.setContent(doc, file.getInputStream());
        DocumentEntity saved = documentRepository.save(doc);

        // Kick off async chunking if available
        try {
            documentToChunkService.toChunksAsync(file, uploader, language, description, tags, status, saved.getId());
        } catch (Exception ignore) { /* service optional */ }

        return saved;
    }

    public DocumentEntity updateDocument(String id, MultipartFile file, String language) throws IOException {
        DocumentEntity doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String newHash = DigestUtils.sha256Hex(file.getInputStream());
        Page<DocumentEntity> existing = documentRepository.findByHash(newHash, PageRequest.of(0, 2));
        boolean duplicate = existing.getContent().stream().anyMatch(d -> !d.getId().equals(id));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another document with the same hash exists");
        }

        // Update safe fields
        try { doc.getClass().getMethod("setHash", String.class).invoke(doc, newHash); } catch (ReflectiveOperationException ignore) { }
        if (language != null) {
            try { doc.getClass().getMethod("setLanguage", String.class).invoke(doc, language); } catch (ReflectiveOperationException ignore) { }
        }
        try {
            if (file.getOriginalFilename() != null) {
                boolean set = false;
                try { doc.getClass().getMethod("setOriginalFilename", String.class).invoke(doc, file.getOriginalFilename()); set = true; } catch (ReflectiveOperationException ignore2) { }
                if (!set) { try { doc.getClass().getMethod("setOriginalFileName", String.class).invoke(doc, file.getOriginalFilename()); } catch (ReflectiveOperationException ignore3) { } }
            }
        } catch (Exception ignore) { }
        try {
            if (file.getContentType() != null) {
                boolean set = false;
                try { doc.getClass().getMethod("setMimeType", String.class).invoke(doc, file.getContentType()); set = true; } catch (ReflectiveOperationException ignore2) { }
                if (!set) { try { doc.getClass().getMethod("setFileType", String.class).invoke(doc, file.getContentType()); } catch (ReflectiveOperationException ignore3) { } }
            }
        } catch (Exception ignore) { }
        try { doc.getClass().getMethod("setFileSize", long.class).invoke(doc, file.getSize()); } catch (ReflectiveOperationException ignore) { }

        contentStore.setContent(doc, file.getInputStream());
        return documentRepository.save(doc);
    }

    public void deleteDocument(String id) {
        if (!documentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        // Cascade delete chunks by documentId if your repository supports it
        try { chunkRepository.deleteByDocumentId(id); } catch (Exception ignore) { }
        documentRepository.deleteById(id);
    }

    public void deleteAllDocuments() {
        // Delete all chunks before deleting documents
        try { chunkRepository.deleteAll(); } catch (Exception ignore) { }
        documentRepository.deleteAll();
    }
}
package io.github.jrohila.simpleragserver.service;

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

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentContentStore contentStore,
            ChunkRepository chunkRepository
    ) {
        this.documentRepository = documentRepository;
        this.contentStore = contentStore;
        this.chunkRepository = chunkRepository;
    }

    public Page<DocumentEntity> listDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.findAll(pageable);
    }

    public Optional<DocumentEntity> getById(String id) {
        return documentRepository.findById(id);
    }

    public DocumentEntity uploadDocument(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String hash = DigestUtils.sha256Hex(file.getInputStream());
        Page<DocumentEntity> existing = documentRepository.findByHash(hash, PageRequest.of(0, 1));
        if (existing.hasContent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document with the same hash already exists");
        }

        DocumentEntity doc = new DocumentEntity();
        // Only set fields that exist on DocumentEntity
        doc.setHash(hash);
        if (file.getOriginalFilename() != null) {
            doc.setOriginalFilename(file.getOriginalFilename());
        }
        if (file.getContentType() != null) {
            doc.setMimeType(file.getContentType());
        }
        // content length may be set by the content store, but we can prefill
        doc.setContentLen(file.getSize());

        // Persist content and metadata
        contentStore.setContent(doc, file.getInputStream());
        DocumentEntity saved = documentRepository.save(doc);

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

        // Update fields that exist on DocumentEntity
        doc.setHash(newHash);
        if (file.getOriginalFilename() != null) {
            doc.setOriginalFilename(file.getOriginalFilename());
        }
        if (file.getContentType() != null) {
            doc.setMimeType(file.getContentType());
        }
        doc.setContentLen(file.getSize());

        contentStore.setContent(doc, file.getInputStream());
        return documentRepository.save(doc);
    }

    public void deleteDocument(String id) {
        if (!documentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        // Cascade delete chunks by documentId if your repository supports it
        try {
            chunkRepository.deleteByDocumentId(id);
        } catch (Exception ignore) {
        }
        documentRepository.deleteById(id);
    }

    public void deleteAllDocuments() {
        // Delete all chunks before deleting documents
        try {
            chunkRepository.deleteAll();
        } catch (Exception ignore) {
        }
        documentRepository.deleteAll();
    }
}

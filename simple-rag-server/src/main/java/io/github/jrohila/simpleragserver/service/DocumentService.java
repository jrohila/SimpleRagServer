package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.entity.DocumentEntity;
import io.github.jrohila.simpleragserver.entity.DocumentEntity.ProcessingState;
import io.github.jrohila.simpleragserver.repository.DocumentRepository;
import io.github.jrohila.simpleragserver.repository.DocumentContentStore;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentContentStore contentStore;
    private final ChunkService chunkService;
    private final ApplicationEventPublisher events;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentContentStore contentStore,
            ChunkService chunkService,
            ApplicationEventPublisher events
    ) {
        this.documentRepository = documentRepository;
        this.contentStore = contentStore;
        this.chunkService = chunkService;
        this.events = events;
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
        
        doc.setState(DocumentEntity.ProcessingState.OPEN);

        // Persist content and metadata
        contentStore.setContent(doc, file.getInputStream());
        DocumentEntity saved = documentRepository.save(doc);
        // publish event to trigger async processing
        try {
            events.publishEvent(new io.github.jrohila.simpleragserver.service.events.DocumentSavedEvent(saved.getId()));
        } catch (Exception ignore) {
        }
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
        DocumentEntity saved = documentRepository.save(doc);
        try {
            events.publishEvent(new io.github.jrohila.simpleragserver.service.events.DocumentSavedEvent(saved.getId()));
        } catch (Exception ignore) {
        }
        return saved;
    }

    public void deleteDocument(String id) {
        if (!documentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        // Cascade delete chunks by documentId if your repository supports it
        try {
            chunkService.deleteByDocumentId(id);
        } catch (Exception ignore) {
        }
        documentRepository.deleteById(id);
    }

    public void deleteAllDocuments() {
        // Delete all chunks before deleting documents
        try {
            chunkService.deleteAll();
        } catch (Exception ignore) {
        }
        documentRepository.deleteAll();
    }

    public DocumentEntity updateProcessingState(String documentId, ProcessingState state) {
        if (documentId == null || documentId.isBlank() || state == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "documentId and state are required");
        }
        DocumentEntity doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        doc.setState(state);
        return documentRepository.save(doc);
    }
}

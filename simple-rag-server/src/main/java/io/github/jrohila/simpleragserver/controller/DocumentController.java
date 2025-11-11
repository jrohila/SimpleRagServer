package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.github.jrohila.simpleragserver.repository.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // List pageable
    @GetMapping
    public List<DocumentEntity> listDocuments(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "collectionId", required = false) String collectionId
    ) {
        log.info("Received request to list documents for collectionId={} page={} size={}", collectionId, page, size);
        return documentService.listDocuments(collectionId, page, size);
    }

    // Get by id
    @GetMapping("/{collectionId}/{id}")
    public ResponseEntity<DocumentEntity> getDocument(@PathVariable String collectionId, @PathVariable String id) {
        log.info("Received request to get document: collectionId={}, id={}", collectionId, id);
        Optional<DocumentEntity> doc = documentService.getById(collectionId, id);
        if (doc.isPresent()) {
            log.info("Document found: collectionId={}, id={}", collectionId, id);
        } else {
            log.info("No document found: collectionId={}, id={}", collectionId, id);
        }
        return doc.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Upload (creates new). Returns 202 if you prefer async processing semantics; here we return 201.
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentEntity> uploadDocument(@RequestParam("collectionId") String collectionId, @RequestParam("file") MultipartFile file
    ) throws IOException {
        log.info("Received request to upload document to collectionId={}", collectionId);
        DocumentEntity saved = documentService.uploadDocument(collectionId, file);
        log.info("Document uploaded: {} to collectionId={}", saved.getId(), collectionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Update file for an existing document
    @PutMapping(path = "/{collectionId}/{id}", consumes = "multipart/form-data")
    public ResponseEntity<DocumentEntity> updateDocument(
            @PathVariable String collectionId,
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language
    ) throws IOException {
        log.info("Received request to update document: collectionId={}, id={}", collectionId, id);
        DocumentEntity saved = documentService.updateDocument(collectionId, id, file, language);
        log.info("Document updated: collectionId={}, id={}", collectionId, id);
        return ResponseEntity.ok(saved);
    }

    // Delete by id
    @DeleteMapping("/{collectionId}/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String collectionId, @PathVariable String id) {
        log.info("Received request to delete document: collectionId={}, id={}", collectionId, id);
        documentService.deleteDocument(collectionId, id);
        log.info("Document deleted: collectionId={}, id={}", collectionId, id);
        return ResponseEntity.noContent().build();
    }

    // Delete all
    @DeleteMapping("/{collectionId}")
    public ResponseEntity<Void> deleteAllDocuments(@PathVariable String collectionId) {
        log.info("Received request to delete all documents in collectionId={}", collectionId);
        documentService.deleteAllDocuments(collectionId);
        log.info("All documents deleted in collectionId={}", collectionId);
        return ResponseEntity.noContent().build();
    }
}

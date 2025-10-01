package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.entity.DocumentEntity;
import io.github.jrohila.simpleragserver.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // List pageable
    @GetMapping
    public Page<DocumentEntity> listDocuments(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return documentService.listDocuments(page, size);
    }

    // Get by id
    @GetMapping("/{id}")
    public ResponseEntity<DocumentEntity> getDocument(@PathVariable String id) {
        Optional<DocumentEntity> doc = documentService.getById(id);
        return doc.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Upload (creates new). Returns 202 if you prefer async processing semantics; here we return 201.
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentEntity> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploader", required = false) String uploader,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "status", required = false) String status
    ) throws IOException {
        DocumentEntity saved = documentService.uploadDocument(file, uploader, language, description, tags, status);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Update file for an existing document
    @PutMapping(path = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<DocumentEntity> updateDocument(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language
    ) throws IOException {
        DocumentEntity saved = documentService.updateDocument(id, file, language);
        return ResponseEntity.ok(saved);
    }

    // Delete by id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    // Delete all
    @DeleteMapping
    public ResponseEntity<Void> deleteAllDocuments() {
        documentService.deleteAllDocuments();
        return ResponseEntity.noContent().build();
    }
}

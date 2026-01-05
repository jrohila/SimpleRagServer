package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.github.jrohila.simpleragserver.repository.DocumentService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;

    @Inject
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // List pageable
        @Get
        public List<DocumentEntity> listDocuments(
            @QueryValue(value = "page", defaultValue = "0") int page,
            @QueryValue(value = "size", defaultValue = "20") int size,
            @QueryValue(value = "collectionId", defaultValue = "") String collectionId
        ) {
        log.info("Received request to list documents for collectionId={} page={} size={}", collectionId, page, size);
        return documentService.listDocuments(collectionId, page, size);
    }

    // Get by id
    @Get("/{collectionId}/{id}")
    public HttpResponse<DocumentEntity> getDocument(@PathVariable String collectionId, @PathVariable String id) {
        log.info("Received request to get document: collectionId={}, id={}", collectionId, id);
        Optional<DocumentEntity> doc = documentService.getById(collectionId, id);
        if (doc.isPresent()) {
            log.info("Document found: collectionId={}, id={}", collectionId, id);
        } else {
            log.info("No document found: collectionId={}, id={}", collectionId, id);
        }
        return doc.map(HttpResponse::ok).orElse(HttpResponse.notFound());
    }

    // Upload (creates new). Returns 202 if you prefer async processing semantics; here we return 201.
    @Post(consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse<DocumentEntity> uploadDocument(@Part("collectionId") String collectionId, @Part("file") CompletedFileUpload file
    ) throws IOException {
        log.info("Received request to upload document to collectionId={}", collectionId);
        DocumentEntity saved = documentService.uploadDocument(collectionId, file);
        log.info("Document uploaded: {} to collectionId={}", saved.getId(), collectionId);
        return HttpResponse.created(saved);
    }

    // Update file for an existing document
    @Put(uri = "/{collectionId}/{id}", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse<DocumentEntity> updateDocument(
            @PathVariable String collectionId,
            @PathVariable String id,
            @Part("file") CompletedFileUpload file,
            @Part(value = "language") String language
    ) throws IOException {
        log.info("Received request to update document: collectionId={}, id={}", collectionId, id);
        DocumentEntity saved = documentService.updateDocument(collectionId, id, file, language);
        log.info("Document updated: collectionId={}, id={}", collectionId, id);
        return HttpResponse.ok(saved);
    }

    // Delete by id
    @Delete(uri = "/{collectionId}/{id}")
    public HttpResponse<Void> deleteDocument(@PathVariable String collectionId, @PathVariable String id) {
        log.info("Received request to delete document: collectionId={}, id={}", collectionId, id);
        documentService.deleteDocument(collectionId, id);
        log.info("Document deleted: collectionId={}, id={}", collectionId, id);
        return HttpResponse.noContent();
    }

    // Delete all
    @Delete(uri = "/{collectionId}")
    public HttpResponse<Void> deleteAllDocuments(@PathVariable String collectionId) {
        log.info("Received request to delete all documents in collectionId={}", collectionId);
        documentService.deleteAllDocuments(collectionId);
        log.info("All documents deleted in collectionId={}", collectionId);
        return HttpResponse.noContent();
    }
}

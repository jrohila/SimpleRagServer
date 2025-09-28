package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.model.DocumentDto;
import io.github.jrohila.simpleragserver.model.ChunkDto;
import io.github.jrohila.simpleragserver.service.PdfToXhtmlConversionService;
import io.github.jrohila.simpleragserver.service.XhtmlStreamLineService;
import io.github.jrohila.simpleragserver.service.XhtmlToChunkService;
import io.github.jrohila.simpleragserver.repository.DocumentDAO;
import io.github.jrohila.simpleragserver.repository.ChunkDAO;
import io.github.jrohila.simpleragserver.dto.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger LOGGER = Logger.getLogger(DocumentController.class.getName());

    @Autowired
    private DocumentDAO documentDAO;
    @Autowired
    private PdfToXhtmlConversionService pdfToXhtmlConversionService;
    @Autowired
    private XhtmlStreamLineService xhtmlStreamLineService;
    @Autowired
    private XhtmlToChunkService xhtmlToChunkService;
    @Autowired
    private ChunkDAO chunkDAO;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentDto> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploader", required = false) String uploader,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "status", required = false) String status
    ) throws IOException {
    long start = System.currentTimeMillis();
    if (file == null) {
        return ResponseEntity.badRequest().build();
    }
    String originalName = file.getOriginalFilename();
    LOGGER.info(String.format(
        "uploadDocument: received file name=%s size=%d contentType=%s uploader=%s language=%s",
        originalName, file.getSize(), file.getContentType(), uploader, language));

        DocumentDto doc = new DocumentDto();
        try {
            // Create and save the document metadata
            doc.setId(java.util.UUID.randomUUID().toString());
            doc.setUploader(uploader);
            doc.setLanguage(language);
            doc.setDescription(description);
            doc.setTags(tags);
            doc.setStatus(status);
            doc.setOriginalFilename(file.getOriginalFilename());
            doc.setFileType(file.getContentType());
            doc.setFileSize(file.getSize());
            doc.setCreatedTime(System.currentTimeMillis());
            doc.setUpdatedTime(System.currentTimeMillis());
            documentDAO.create(doc);
        LOGGER.info(String.format("uploadDocument: saved document metadata id=%s name=%s size=%d",
            doc.getId(), doc.getOriginalFilename(), doc.getFileSize()));

            // Extract chunks using PDF to XHTML pipeline
            String xhtml = pdfToXhtmlConversionService.parseToXhtml(file.getBytes());
            String streamlined = xhtmlStreamLineService.streamlineParagraphs(xhtml);
            List<ChunkDto> chunks = xhtmlToChunkService.parseChunks(streamlined);
            LOGGER.info(String.format("uploadDocument: extracted chunks=%d", chunks.size()));
            int saved = 0;
            int total = chunks.size();
            for (ChunkDto chunkDto : chunks) {
                chunkDto.setDocumentId(doc.getId());
                chunkDAO.create(chunkDto);
                saved++;
                if (saved == 1 || saved == total || saved % 10 == 0) {
                    LOGGER.info(String.format("uploadDocument: chunk progress %d/%d (docId=%s)", saved, total, doc.getId()));
                }
            }
            LOGGER.info(String.format("uploadDocument: saved chunks=%d/%d for document id=%s", saved, total, doc.getId()));
            return ResponseEntity.ok(doc);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "uploadDocument: failure while processing document id=" + doc.getId()
                    + ", name=" + originalName, ex);
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - start;
            LOGGER.info(String.format("uploadDocument: completed for id=%s, durationMs=%d",
                    doc.getId(), duration));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> getDocument(@PathVariable String id) {
    java.util.Optional<DocumentDto> docOpt = documentDAO.findById(id);
        return docOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<DocumentDto> listDocuments() {
    return documentDAO.findAll();
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentDto> updateDocument(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploader", required = false) String uploader,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "status", required = false) String status
    ) throws IOException {
    java.util.Optional<DocumentDto> docOpt = documentDAO.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DocumentDto existingDoc = docOpt.get();
        // Update document fields
        existingDoc.setUploader(uploader);
        existingDoc.setLanguage(language);
        existingDoc.setDescription(description);
        existingDoc.setTags(tags);
        existingDoc.setStatus(status);
        existingDoc.setOriginalFilename(file.getOriginalFilename());
        existingDoc.setFileType(file.getContentType());
        existingDoc.setFileSize(file.getSize());
        existingDoc.setUpdatedTime(System.currentTimeMillis());
        // Save updated document
    DocumentDto saved = documentDAO.save(existingDoc);

        // Delete all chunks referencing this document
        chunkDAO.deleteAllByDocumentId(id);

        // Extract and save new chunks
        try {
            String xhtml = pdfToXhtmlConversionService.parseToXhtml(file.getBytes());
            String streamlined = xhtmlStreamLineService.streamlineParagraphs(xhtml);
            List<ChunkDto> chunks = xhtmlToChunkService.parseChunks(streamlined);
            for (ChunkDto chunkDto : chunks) {
                chunkDto.setDocumentId(saved.getId());
                chunkDAO.create(chunkDto);
            }
        } catch (IOException ex) {
            Logger.getLogger(DocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) {
    if (documentDAO.existsById(id)) {
            // Delete all chunks referencing this document using ChunkDAO
            chunkDAO.deleteAllByDocumentId(id);
            documentDAO.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllDocuments() {
        long start = System.currentTimeMillis();
        List<DocumentDto> docs = documentDAO.findAll();
        int total = docs.size();
        if (total == 0) {
            LOGGER.info("deleteAllDocuments: no documents to delete");
            return ResponseEntity.noContent().build();
        }
        LOGGER.info("deleteAllDocuments: deleting total documents=" + total);
        int processed = 0;
        for (DocumentDto doc : docs) {
            try {
                chunkDAO.deleteAllByDocumentId(doc.getId());
                documentDAO.deleteById(doc.getId());
                processed++;
                if (processed == 1 || processed == total || processed % 25 == 0) {
                    LOGGER.info(String.format("deleteAllDocuments: progress %d/%d (lastId=%s)", processed, total, doc.getId()));
                }
            } catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, "deleteAllDocuments: failure deleting document id=" + doc.getId(), ex);
            }
        }
        long duration = System.currentTimeMillis() - start;
        LOGGER.info(String.format("deleteAllDocuments: completed deleted=%d/%d durationMs=%d", processed, total, duration));
        return ResponseEntity.noContent().build();
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.pipeline;

import io.github.jrohila.simpleragserver.pipeline.XhtmlCleanup;
import io.github.jrohila.simpleragserver.pipeline.XhtmlToChunk;
import io.github.jrohila.simpleragserver.pipeline.PdfToXhtmlConversion;
import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import io.github.jrohila.simpleragserver.repository.ChunkRepository;
import io.github.jrohila.simpleragserver.client.EmbedClient;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Lenovo
 */
@Component
public class DocumentChunker {

    private static final Logger LOGGER = Logger.getLogger(DocumentChunker.class.getName());

    @Autowired
    private EmbedClient embedService;

    @Autowired
    private PdfToXhtmlConversion pdfToXhtmlConversionService;
    @Autowired
    private XhtmlCleanup xhtmlStreamLineService;
    @Autowired
    private XhtmlToChunk xhtmlToChunkService;
    @Autowired
    private ChunkRepository chunkDAO;

    public void toChunks(MultipartFile file,
            String uploader,
            String language,
            String description,
            List<String> tags,
            String status,
            String documentId) {
        long start = System.currentTimeMillis();

        String originalName = file.getOriginalFilename();
        try {
            // Extract chunks using PDF to XHTML pipeline
            String xhtml = pdfToXhtmlConversionService.parseToXhtml(file.getBytes());
            String streamlined = xhtmlStreamLineService.streamlineParagraphs(xhtml);
            List<ChunkEntity> chunks = xhtmlToChunkService.parseChunks(streamlined);
            LOGGER.info(String.format("uploadDocument: extracted chunks=%d", chunks.size()));
            int saved = 0;
            int total = chunks.size();
            for (ChunkEntity chunk : chunks) {
                chunk.setDocumentId(documentId); // Set reference to parent document
                chunk.setEmbedding(embedService.getEmbeddingAsList(chunk.getSectionTitle() + " : " + chunk.getText()));
                chunkDAO.save(chunk);
                saved++;
                if (saved == 1 || saved == total || saved % 10 == 0) {
                    LOGGER.info(String.format("uploadDocument: chunk progress %d/%d (docId=%s)", saved, total, documentId));
                }
            }
            LOGGER.info(String.format("uploadDocument: saved chunks=%d/%d for document id=%s", saved, total, documentId));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "uploadDocument: failure while processing document id=" + documentId + ", name=" + originalName, ex);
            throw ex;
        } catch (IOException ex) {
            System.getLogger(DocumentChunker.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } finally {
            long duration = System.currentTimeMillis() - start;
            LOGGER.info(String.format("uploadDocument: completed for id=%s, durationMs=%d", documentId, duration));
        }
    }

    @Async("chunkingExecutor")
    public void toChunksAsync(MultipartFile file,
                              String uploader,
                              String language,
                              String description,
                              java.util.List<String> tags,
                              String status,
                              String documentId) {
        // Delegate to the existing sync method
        this.toChunks(file, uploader, language, description, tags, status, documentId);
    }

}

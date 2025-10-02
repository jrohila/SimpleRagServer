/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.pipeline;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import io.github.jrohila.simpleragserver.client.EmbedClient;
import io.github.jrohila.simpleragserver.repository.DocumentContentStore;
import io.github.jrohila.simpleragserver.repository.DocumentRepository;
import io.github.jrohila.simpleragserver.service.ChunkService;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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
    private ChunkService chunkService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentContentStore documentContentStore;

    @Async("chunkingExecutor")
    public void asyncProcess(String documentId) {
        this.process(documentId);
    }

    public void process(String documentId) {
        long start = System.currentTimeMillis();
        try {
            var docOpt = documentRepository.findById(documentId);
            if (docOpt.isEmpty()) {
                LOGGER.log(Level.WARNING, "DocumentChunker.process: document not found id={0}", documentId);
                return;
            }
            var doc = docOpt.get();

            var in = documentContentStore.getContent(doc);
            if (in == null) {
                LOGGER.log(Level.WARNING, "DocumentChunker.process: no content for id={0}", documentId);
                return;
            }
            byte[] bytes = in.readAllBytes();

            String xhtml = pdfToXhtmlConversionService.parseToXhtml(bytes);
            String streamlined = xhtmlStreamLineService.streamlineParagraphs(xhtml);
            List<ChunkEntity> chunks = xhtmlToChunkService.parseChunks(streamlined);
            LOGGER.info(String.format("DocumentChunker.process: extracted chunks=%d", chunks.size()));
            int saved = 0;
            int total = chunks.size();
            for (ChunkEntity chunk : chunks) {
                chunk.setDocumentId(documentId);
                if (chunk.getHash() != null && !chunk.getHash().isBlank()) {
                    chunk.setId(documentId + ":" + chunk.getHash());
                }

                // Build embedding input; skip if no text to embed
                String embedInput = (chunk.getSectionTitle() == null ? "" : chunk.getSectionTitle()) +
                        (chunk.getSectionTitle() == null ? "" : " : ") +
                        (chunk.getText() == null ? "" : chunk.getText());
                if (embedInput.isBlank()) {
                    LOGGER.log(Level.FINE, "Skipping chunk without content (no embedding input). docId={0}", documentId);
                    continue;
                }

                // Compute embedding
                try {
                    chunk.setEmbedding(embedService.getEmbeddingAsList(embedInput));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Embedding failed for a chunk; skipping. docId=" + documentId, e);
                    continue;
                }

                // Persist via service; on validation failure, skip and continue
                try {
                    if (chunk.getId() != null && chunkService.getById(chunk.getId()).isPresent()) {
                        chunkService.update(chunk.getId(), chunk);
                    } else {
                        chunkService.create(chunk);
                    }
                    saved++;
                    if (saved == 1 || saved == total || saved % 10 == 0) {
                        LOGGER.info(String.format("DocumentChunker.process: chunk progress %d/%d (docId=%s)", saved, total, documentId));
                    }
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    LOGGER.log(Level.WARNING, "Skipping invalid/duplicate chunk. docId=" + documentId + " reason=" + ex.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "DocumentChunker.process: IO error for id=" + documentId, e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "DocumentChunker.process: processing failed for id=" + documentId, e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            LOGGER.info("DocumentChunker.process: completed id=" + documentId + ", ms=" + duration);
        }
    }

}

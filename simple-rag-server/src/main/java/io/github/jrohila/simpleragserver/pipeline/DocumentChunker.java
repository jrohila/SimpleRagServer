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

            // Load content stream from content store
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
                chunk.setDocumentId(documentId); // Set reference to parent document
                // Deterministic ID to avoid duplicates: hash
                chunk.setId(chunk.getHash());
                //
                chunk.setEmbedding(embedService.getEmbeddingAsList(chunk.getSectionTitle() + " : " + chunk.getText()));
                
                // Use ChunkService for persistence (create or update if already exists by id)
                if (chunk.getId() != null && chunkService.getById(chunk.getId()).isPresent()) {
                    chunkService.update(chunk.getId(), chunk);
                } else {
                    chunkService.create(chunk);
                }

                saved++;
                if (saved == 1 || saved == total || saved % 10 == 0) {
                    LOGGER.info(String.format("DocumentChunker.process: chunk progress %d/%d (docId=%s)", saved, total, documentId));
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

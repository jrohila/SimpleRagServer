/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import io.github.jrohila.simpleragserver.client.EmbedClient;
import io.github.jrohila.simpleragserver.client.DoclingClient;
import io.github.jrohila.simpleragserver.client.DoclingAsyncClient;
import io.github.jrohila.simpleragserver.dto.DoclingChunkRequest;
import io.github.jrohila.simpleragserver.dto.DoclingChunkResponse;
import io.github.jrohila.simpleragserver.repository.DocumentContentStore;
import io.github.jrohila.simpleragserver.repository.DocumentRepository;
// imports for ChunkService and NlpService are unnecessary since they're in the same package
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Lenovo
 */
@Component
public class DocumentChunkerService {

    private static final Logger LOGGER = Logger.getLogger(DocumentChunkerService.class.getName());

    @Autowired
    private EmbedClient embedService;

    @Autowired
    private DoclingClient doclingClient;

    @Autowired
    private DoclingAsyncClient doclingAsyncClient;

    @Value("${processing.chunking:async}")
    private String chunkingMode;

    // Legacy XHTML pipeline services retained for potential fallback are now unused

    @Autowired
    private ChunkService chunkService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentContentStore documentContentStore;

    @Autowired
    private NlpService nlpService;

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
            // Use Docling hybrid chunker instead of local XHTML-based pipeline
            DoclingChunkRequest.HybridChunkerOptions opts = new DoclingChunkRequest.HybridChunkerOptions();
            opts.setUseMarkdownTables(false);
            opts.setIncludeRawText(false);
            opts.setMaxTokens(384); // sensible default
            opts.setMergePeers(true);
            // Let tokenizer default to docling's default

        DoclingChunkResponse resp;
        boolean useAsync = chunkingMode == null || chunkingMode.isBlank() || chunkingMode.equalsIgnoreCase("async");
        if (useAsync) {
        var startRes = doclingAsyncClient.hybridChunkFromBytes(
            doc.getOriginalFilename() != null ? doc.getOriginalFilename() : (documentId + ".pdf"),
            bytes,
            opts,
            false,
            "inbody",
            null
        );
        var polled = doclingAsyncClient.pollChunkUntilTerminal(
            startRes.operationId() != null ? startRes.operationId() : startRes.pollUrl(),
            1200_000L,
            500L
        );
        if (polled.status() == null || !polled.status().isSuccess()) {
            LOGGER.warning("Async Docling chunking did not complete successfully; falling back to sync.");
            resp = doclingClient.hybridChunkFromBytes(
                doc.getOriginalFilename() != null ? doc.getOriginalFilename() : (documentId + ".pdf"),
                bytes,
                opts,
                false,
                "inbody",
                null
            );
        } else {
            resp = polled.response();
            if (resp == null || resp.getChunks() == null) {
            LOGGER.warning("Async Docling returned empty response; falling back to sync.");
            resp = doclingClient.hybridChunkFromBytes(
                doc.getOriginalFilename() != null ? doc.getOriginalFilename() : (documentId + ".pdf"),
                bytes,
                opts,
                false,
                "inbody",
                null
            );
            }
        }
        } else {
        resp = doclingClient.hybridChunkFromBytes(
            doc.getOriginalFilename() != null ? doc.getOriginalFilename() : (documentId + ".pdf"),
            bytes,
            opts,
            false, // include_converted_doc
            "inbody",
            null // use default convert options
        );
        }

            List<DoclingChunkResponse.Chunk> doclingChunks = resp.getChunks();
            if (doclingChunks == null || doclingChunks.isEmpty()) {
                LOGGER.info("DocumentChunker.process: Docling returned no chunks, skipping.");
                return;
            }

            List<ChunkEntity> chunks = new java.util.ArrayList<>();
            for (DoclingChunkResponse.Chunk c : doclingChunks) {
                ChunkEntity e = new ChunkEntity();
                e.setText(c.getText());
                e.setSectionTitle(c.getTitle());
                if (c.getPageNumber() != null) e.setPageNumber(c.getPageNumber());
                // Detect language based on text content
                try {
                    String lang = nlpService.detectLanguage(e.getText());
                    e.setLanguage(lang);
                } catch (RuntimeException ex) {
                    e.setLanguage("und");
                }
                // carry doc name and id later during save loop
                // language/hash not provided by Docling chunks; leave empty
                chunks.add(e);
            }
            LOGGER.info(String.format("DocumentChunker.process: docling extracted chunks=%d", chunks.size()));
            // Apply quality gate filters (e.g., remove 'und' language chunks)
            int saved = 0;
            int total = chunks.size();
            for (ChunkEntity chunk : chunks) {
                chunk.setDocumentId(documentId);
                // Copy original file name into chunk as documentName for denormalized display
                chunk.setDocumentName(doc.getOriginalFilename());
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
                    LOGGER.log(Level.WARNING, "Skipping invalid/duplicate chunk. docId={0} reason={1}", new Object[]{documentId, ex.getMessage()});
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "DocumentChunker.process: IO error for id=" + documentId, e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "DocumentChunker.process: processing failed for id=" + documentId, e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            LOGGER.log(Level.INFO, "DocumentChunker.process: completed id={0}, ms={1}", new Object[]{documentId, duration});
        }
    }

}

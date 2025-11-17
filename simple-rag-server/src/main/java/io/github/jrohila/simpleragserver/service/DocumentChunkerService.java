/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.repository.ChunkService;
import io.github.jrohila.simpleragserver.repository.DocumentService;
import io.github.jrohila.simpleragserver.repository.ChunkingTaskService;
import io.github.jrohila.simpleragserver.domain.ChunkEntity;
import io.github.jrohila.simpleragserver.domain.ChunkingTaskEntity;
import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.github.jrohila.simpleragserver.event.DocumentUploadEvent;
import io.github.jrohila.simpleragserver.client.EmbedClient;
import io.github.jrohila.simpleragserver.client.DoclingAsyncClient;
import io.github.jrohila.simpleragserver.domain.DoclingChunkRequest;
import io.github.jrohila.simpleragserver.domain.DoclingChunkResponse;
import io.github.jrohila.simpleragserver.pipeline.ChunkQualityGate;
import io.github.jrohila.simpleragserver.repository.DocumentContentStore;
// imports for ChunkService and NlpService are unnecessary since they're in the same package
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    private DoclingAsyncClient doclingAsyncClient;

    @Autowired
    private ChunkService chunkService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentContentStore documentContentStore;

    @Autowired
    private NlpService nlpService;

    @Autowired
    private ChunkQualityGate qualityGate;

    @Autowired
    private ChunkingTaskService chunkingTaskService;

    @Scheduled(fixedDelay = 60000)
    public void checkChunkingProcessState() {
        LOGGER.info("DocumentChunker: Running scheduled task");

        List<ChunkingTaskEntity> tasks = this.chunkingTaskService.findByStatus(DocumentEntity.ProcessingState.PROCESSING, 0, 25);
        LOGGER.log(Level.INFO, "DocumentChunker: Found {0} tasks in PROCESSING state", tasks.size());

        for (ChunkingTaskEntity task : tasks) {
            LOGGER.log(Level.INFO, "DocumentChunker: Checking task status for taskId={0}, documentId={1}", new Object[]{task.getTaskId(), task.getDocumentId()});

            DoclingAsyncClient.OperationStatus status = this.checkIsChunkingDone(task.getTaskId());
            if (status.isSuccess()) {
                LOGGER.log(Level.INFO, "DocumentChunker: Task completed successfully, taskId={0}", task.getTaskId());
                try {
                    this.saveChunks(task.getCollectionId(), task.getDocumentId(), task.getTaskId());
                    this.chunkingTaskService.updateStatus(task.getId(), DocumentEntity.ProcessingState.DONE);
                    LOGGER.log(Level.INFO, "DocumentChunker: Chunks saved and task marked as DONE, documentId={0}", task.getDocumentId());
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "DocumentChunker: Failed to save chunks for documentId=" + task.getDocumentId(), ex);
                    this.chunkingTaskService.updateStatus(task.getId(), DocumentEntity.ProcessingState.FAILED);
                }
            } else {
                LOGGER.log(Level.FINE, "DocumentChunker: Task still processing, taskId={0}, status={1}", new Object[]{task.getTaskId(), status});
            }
        }
    }

    @EventListener
    public void handleDocumentUpload(DocumentUploadEvent event) {
        this.startChunkingProcess(event.getCollectionId(), event.getDocumentId());
    }

    public DocumentEntity.ProcessingState startChunkingProcess(String collectionId, String documentId) {
        LOGGER.log(Level.INFO, "DocumentChunker: Starting chunking process for collectionId={0}, documentId={1}",
                new Object[]{collectionId, documentId});

        try {
            var docOpt = documentService.getById(collectionId, documentId);
            if (docOpt.isEmpty()) {
                LOGGER.log(Level.WARNING, "DocumentChunker.process: document not found id={0}", documentId);
                return DocumentEntity.ProcessingState.FAILED;
            }
            var doc = docOpt.get();

            var in = documentContentStore.getContent(doc);
            if (in == null) {
                LOGGER.log(Level.WARNING, "DocumentChunker.process: no content for id={0}", documentId);
                return DocumentEntity.ProcessingState.FAILED;
            }

            byte[] bytes = in.readAllBytes();
            LOGGER.log(Level.INFO, "DocumentChunker: Read {0} bytes for documentId={1}",
                    new Object[]{bytes.length, documentId});

            // Use Docling hybrid chunker instead of local XHTML-based pipeline
            DoclingChunkRequest.HybridChunkerOptions opts = new DoclingChunkRequest.HybridChunkerOptions();
            opts.setUseMarkdownTables(false);
            opts.setIncludeRawText(false);
            opts.setMaxTokens(384); // sensible default
            opts.setMergePeers(true);
            // Let tokenizer default to docling's default

            LOGGER.log(Level.INFO, "DocumentChunker: Submitting document to Docling async chunker, documentId={0}", documentId);

            var response = doclingAsyncClient.hybridChunkFromBytes(
                    doc.getOriginalFilename() != null ? doc.getOriginalFilename() : (documentId + ".pdf"),
                    bytes,
                    opts,
                    false,
                    "inbody",
                    null
            );

            LOGGER.log(Level.INFO, "DocumentChunker: Received operation ID from Docling, taskId={0}, documentId={1}",
                    new Object[]{response.operationId(), documentId});

            ChunkingTaskEntity task = new ChunkingTaskEntity();
            task.setCollectionId(collectionId);
            task.setDocumentId(documentId);
            task.setTaskId(response.operationId());
            task.setStatus(DocumentEntity.ProcessingState.PROCESSING);

            chunkingTaskService.save(task);
            LOGGER.log(Level.INFO, "DocumentChunker: Chunking task created and saved, taskId={0}", response.operationId());

            return DocumentEntity.ProcessingState.PROCESSING;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "DocumentChunker: IOException while starting chunking process for documentId=" + documentId, ex);
        }
        return DocumentEntity.ProcessingState.FAILED;
    }

    private DoclingAsyncClient.OperationStatus checkIsChunkingDone(String pollUrlOrId) {
        LOGGER.log(Level.FINE, "DocumentChunker: Checking status for taskId={0}", pollUrlOrId);
        return doclingAsyncClient.getStatusById(pollUrlOrId);
    }

    private void saveChunks(String collectionId, String documentId, String pollUrlOrId) throws Exception {
        LOGGER.log(Level.INFO, "DocumentChunker: Starting to save chunks for documentId={0}, taskId={1}", new Object[]{documentId, pollUrlOrId});

        var docOpt = documentService.getById(collectionId, documentId);
        if (docOpt.isEmpty()) {
            LOGGER.log(Level.WARNING, "DocumentChunker.process: document not found id={0}", documentId);
            throw new Exception("DocumentChunker.process: document not found id=" + documentId);
        }
        var doc = docOpt.get();

        LOGGER.log(Level.INFO, "DocumentChunker: Polling Docling for chunks, taskId={0}", pollUrlOrId);
        var polled = this.doclingAsyncClient.pollChunkUntilTerminal(pollUrlOrId, 60000, 1000);
        if (polled != null && polled.status().isSuccess()) {
            DoclingChunkResponse resp = polled.response();
            if (resp == null) {
                LOGGER.log(Level.WARNING, "DocumentChunker: Docling returned null response for documentId={0}", documentId);
                throw new Exception("DocumentChunker: Docling returned null response for documentId=" + documentId);
            }
            List<DoclingChunkResponse.Chunk> doclingChunks = resp.getChunks();
            if (doclingChunks == null || doclingChunks.isEmpty()) {
                LOGGER.log(Level.WARNING, "DocumentChunker: Docling returned no chunks for documentId={0}", documentId);
                return;
            }
            LOGGER.log(Level.INFO, "DocumentChunker: Received {0} chunks from Docling for documentId={1}", new Object[]{doclingChunks.size(), documentId});

            List<ChunkEntity> chunks = new java.util.ArrayList<>();
            for (DoclingChunkResponse.Chunk c : doclingChunks) {
                ChunkEntity e = new ChunkEntity();
                e.setText(c.getText());
                e.setSectionTitle(c.getTitle());
                if (c.getPageNumber() != null) {
                    e.setPageNumber(c.getPageNumber());
                }
                // Detect language based on text content
                try {
                    String lang = nlpService.detectLanguage(e.getText());
                    e.setLanguage(lang);
                    LOGGER.log(Level.FINE, "DocumentChunker: Detected language={0} for chunk in documentId={1}", new Object[]{lang, documentId});
                } catch (RuntimeException ex) {
                    LOGGER.log(Level.WARNING, "DocumentChunker: Failed to detect language for chunk in documentId=" + documentId, ex);
                    e.setLanguage("und");
                }
                // carry doc name and id later during save loop
                // language/hash not provided by Docling chunks; leave empty
                chunks.add(e);
            }

            LOGGER.log(Level.INFO, "DocumentChunker: Processed {0} chunks for documentId={1}", new Object[]{chunks.size(), documentId});

            // Apply quality gate filters (e.g., remove 'und' language chunks)
            int saved = 0;
            int total = chunks.size();
            LOGGER.log(Level.INFO, "DocumentChunker: Starting to persist {0} chunks for documentId={1}", new Object[]{total, documentId});

            for (ChunkEntity chunk : chunks) {
                chunk.setDocumentId(documentId);
                // Copy original file name into chunk as documentName for denormalized display
                chunk.setDocumentName(doc.getOriginalFilename());
                if (chunk.getHash() != null && !chunk.getHash().isBlank()) {
                    chunk.setId(documentId + ":" + chunk.getHash());
                }

                // Build embedding input; skip if no text to embed
                String embedInput = (chunk.getSectionTitle() == null ? "" : chunk.getSectionTitle())
                        + (chunk.getSectionTitle() == null ? "" : " : ")
                        + (chunk.getText() == null ? "" : chunk.getText());
                if (embedInput.isBlank()) {
                    LOGGER.log(Level.FINE, "Skipping chunk without content (no embedding input). docId={0}", documentId);
                    continue;
                }

                // Quality gate: require at least 3 sentences
                if (!qualityGate.filter(chunk)) {
                    LOGGER.log(Level.FINE, "Skipping chunk that failed quality gate. docId={0}", documentId);
                    continue;
                }
                
                chunk.setType(chunk.getType());
                chunk.setPageNumber(chunk.getPageNumber());
                chunk.setSectionTitle(chunk.getSectionTitle());
                
                chunk.setEmbedding(embedService.getEmbeddingAsList(embedInput));

                // Persist via service; on validation failure, skip and continue
                try {
                    if (chunk.getId() != null && chunkService.getById(collectionId, chunk.getId()).isPresent()) {
                        chunkService.update(collectionId, chunk.getId(), chunk);
                    } else {
                        chunkService.create(collectionId, chunk);
                    }
                    saved++;
                    if (saved == 1 || saved == total || saved % 10 == 0) {
                        LOGGER.log(Level.INFO, "DocumentChunker: chunk progress {0}/{1} (docId={2})", new Object[]{saved, total, documentId});
                    }
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    LOGGER.log(Level.WARNING, "Skipping invalid/duplicate chunk. docId={0} reason={1}", new Object[]{documentId, ex.getMessage()});
                }
            }

            this.documentService.updateProcessingState(collectionId, doc.getId(), DocumentEntity.ProcessingState.DONE);
            LOGGER.log(Level.INFO, "DocumentChunker: Successfully saved {0}/{1} chunks for documentId={2}", new Object[]{saved, total, documentId});
        } else {
            this.documentService.updateProcessingState(collectionId, doc.getId(), DocumentEntity.ProcessingState.FAILED);
            LOGGER.log(Level.WARNING, "DocumentChunker: Polling failed or returned no success status for taskId={0}, documentId={1}", new Object[]{pollUrlOrId, documentId});
        }
    }

}

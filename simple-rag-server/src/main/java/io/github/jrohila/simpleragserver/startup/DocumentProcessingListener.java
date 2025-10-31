package io.github.jrohila.simpleragserver.startup;

import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.github.jrohila.simpleragserver.service.ChunkSearchService;
import io.github.jrohila.simpleragserver.service.DocumentChunkerService;
import io.github.jrohila.simpleragserver.service.DocumentService;
import io.github.jrohila.simpleragserver.service.events.DocumentSavedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class DocumentProcessingListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingListener.class);

    private final DocumentService documentService;
    private final DocumentChunkerService documentChunker;

    public DocumentProcessingListener(DocumentService documentService, DocumentChunkerService documentChunker) {
        this.documentService = documentService;
        this.documentChunker = documentChunker;
    }

    @Async("chunkingExecutor")
    @EventListener
    public void onDocumentSaved(DocumentSavedEvent evt) {
        String documentId = evt.documentId();
        try {
            // mark as processing
            documentService.updateProcessingState(documentId, DocumentEntity.ProcessingState.PROCESSING);
            // process
            documentChunker.process(documentId);
            // mark as done
            documentService.updateProcessingState(documentId, DocumentEntity.ProcessingState.DONE);
        } catch (Exception ex) {
            log.error("Exception during document processing for id {}: {}", documentId, ex.getMessage(), ex);
            documentService.updateProcessingState(documentId, DocumentEntity.ProcessingState.FAILED);
        }
    }
}

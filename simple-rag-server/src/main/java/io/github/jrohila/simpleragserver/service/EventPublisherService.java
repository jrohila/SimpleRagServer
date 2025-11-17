package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.event.DocumentUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisherService.class);

    private final ApplicationEventPublisher eventPublisher;

    public EventPublisherService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishDocumentUploadEvent(String collectionId, String documentId) {
        DocumentUploadEvent event = new DocumentUploadEvent(collectionId, documentId);
        eventPublisher.publishEvent(event);
        logger.info("Published DocumentUploadEvent: collectionId={}, documentId={}", collectionId, documentId);
    }
}

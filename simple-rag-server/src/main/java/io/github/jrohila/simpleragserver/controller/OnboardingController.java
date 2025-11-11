package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.ChatEntity;
import io.github.jrohila.simpleragserver.domain.CollectionEntity;
import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.github.jrohila.simpleragserver.domain.OnboardingDTO;
import io.github.jrohila.simpleragserver.repository.ChatManagerService;
import io.github.jrohila.simpleragserver.repository.CollectionService;
import io.github.jrohila.simpleragserver.repository.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private static final Logger log = LoggerFactory.getLogger(OnboardingController.class);

    private final ChatManagerService chatManagerService;
    private final CollectionService collectionService;
    private final DocumentService documentService;

    @Autowired
    public OnboardingController(ChatManagerService chatManagerService,
            CollectionService collectionManagerService,
            DocumentService documentService) {
        this.chatManagerService = chatManagerService;
        this.collectionService = collectionManagerService;
        this.documentService = documentService;
    }

    @PostMapping(value = "/createNewChat", consumes = "multipart/form-data")
    public ResponseEntity<OnboardingResponse> createNewChat(
            @RequestParam String publicName,
            @RequestParam String internalName,
            @RequestParam String internalDescription,
            @RequestParam String defaultLanguage,
            @RequestParam String defaultSystemPrompt,
            @RequestParam String defaultSystemPromptAppend,
            @RequestParam String defaultContextPrompt,
            @RequestParam String defaultMemoryPrompt,
            @RequestParam String defaultExtractorPrompt,
            @RequestParam String defaultOutOfScopeContext,
            @RequestParam String defaultOutOfScopeMessage,
            @RequestParam String collectionName,
            @RequestParam String collectionDescription,
            @RequestParam(defaultValue = "true") boolean overrideSystemMessage,
            @RequestParam(defaultValue = "true") boolean overrideAssistantMessage,
            @RequestParam(value = "file", required = false) List<MultipartFile> files) {
        
        log.info("Starting onboarding for chat: publicName={}, collectionName={}", publicName, collectionName);
        int fileCount = files != null ? files.size() : 0;
        log.debug("Received {} files for upload", fileCount);
        
        // Map RequestParams to ChatEntity
        ChatEntity chat = new ChatEntity();
        chat.setPublicName(publicName);
        chat.setInternalName(internalName);
        chat.setInternalDescription(internalDescription);
        chat.setDefaultLanguage(defaultLanguage);
        chat.setDefaultSystemPrompt(defaultSystemPrompt);
        chat.setDefaultSystemPromptAppend(defaultSystemPromptAppend);
        chat.setDefaultContextPrompt(defaultContextPrompt);
        chat.setDefaultMemoryPrompt(defaultMemoryPrompt);
        chat.setDefaultExtractorPrompt(defaultExtractorPrompt);
        chat.setOverrideSystemMessage(overrideSystemMessage);
        chat.setOverrideAssistantMessage(overrideAssistantMessage);
        chat.setDefaultOutOfScopeMessage(defaultOutOfScopeMessage);

        // Map RequestParams to CollectionEntity
        CollectionEntity collection = new CollectionEntity();
        collection.setName(collectionName);
        collection.setDescription(collectionDescription);
        log.debug("Creating collection: {}", collectionName);
        CollectionEntity createdCollection = collectionService.create(collection);
        log.info("Collection created successfully: id={}, name={}", createdCollection.getId(), createdCollection.getName());

        // Save chat
        chat.setDefaultCollectionId(createdCollection.getId());
        log.debug("Creating chat: {}", publicName);
        ChatEntity createdChat = chatManagerService.create(chat);
        log.info("Chat created successfully: id={}, publicName={}", createdChat.getId(), createdChat.getPublicName());

        // Save documents
        List<DocumentEntity> createdDocs = new ArrayList<>();
        if (files != null) {
            log.info("Processing {} files for collection id={}", files.size(), createdCollection.getId());
            for (MultipartFile file : files) {
                try {
                    log.debug("Uploading file: name={}, size={} bytes", file.getOriginalFilename(), file.getSize());
                    DocumentEntity doc = documentService.uploadDocument(createdCollection.getId(), file);
                    createdDocs.add(doc);
                    log.info("File uploaded successfully: id={}, originalFilename={}", doc.getId(), doc.getOriginalFilename());
                } catch (Exception e) {
                    log.error("Failed to upload file: name={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
                }
            }
        } else {
            log.info("No files provided for upload");
        }
        
        log.info("Onboarding completed successfully: chatId={}, collectionId={}, documentsCount={}", 
                createdChat.getId(), createdCollection.getId(), createdDocs.size());
        OnboardingResponse response = new OnboardingResponse(createdChat, createdCollection, createdDocs);
        return ResponseEntity.ok(response);
    }

    public static class OnboardingResponse {

        public ChatEntity chat;
        public CollectionEntity collection;
        public List<DocumentEntity> documents;

        public OnboardingResponse(ChatEntity chat, CollectionEntity collection, List<DocumentEntity> documents) {
            this.chat = chat;
            this.collection = collection;
            this.documents = documents;
        }
    }
}

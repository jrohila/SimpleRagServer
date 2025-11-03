package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.ChatEntity;
import io.github.jrohila.simpleragserver.domain.CollectionEntity;
import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.github.jrohila.simpleragserver.domain.OnboardingDTO;
import io.github.jrohila.simpleragserver.repository.ChatManagerService;
import io.github.jrohila.simpleragserver.repository.CollectionService;
import io.github.jrohila.simpleragserver.repository.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

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
        @RequestParam String collectionName,
        @RequestParam String collectionDescription,
        @RequestParam(defaultValue = "true") boolean overrideSystemMessage,
        @RequestParam(defaultValue = "true") boolean overrideAssistantMessage,
        @RequestParam(value = "file", required = false) List<MultipartFile> files) {
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

        // Map RequestParams to CollectionEntity
        CollectionEntity collection = new CollectionEntity();
        collection.setName(collectionName);
        collection.setDescription(collectionDescription);
        CollectionEntity createdCollection = collectionService.create(collection);

        // Save chat
        chat.setDefaultCollectionId(createdCollection.getId());
        ChatEntity createdChat = chatManagerService.create(chat);

        // Save documents
        List<DocumentEntity> createdDocs = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                try {
                    DocumentEntity doc = documentService.uploadDocument(createdCollection.getId(), file);
                    createdDocs.add(doc);
                } catch (Exception e) {
                    // Optionally handle or log file upload errors
                }
            }
        }
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

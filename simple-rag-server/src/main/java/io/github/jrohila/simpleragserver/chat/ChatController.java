package io.github.jrohila.simpleragserver.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jrohila.simpleragserver.repository.ChatManagerService;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class ChatController {

    @Autowired
    private ChatManagerService chatManagerService;
    
    private final ChatService chatService;
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    
    @Value("${spring.ai.ollama.chat.options.model}")
    private String defaultModel;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // OpenAI-compatible chat completions endpoint
    @PostMapping(path = {"/{publicName}/v1/chat/completions", "/{publicName}/api/chat"})
    public ResponseEntity<?> createCompletion(@PathVariable String publicName,
                                             @RequestBody OpenAiChatRequest request,
                                             @RequestParam(value = "useRag", required = false) Boolean useRag) {
        try {
            // Fetch ChatEntity by publicName
            var chatEntityOpt = chatManagerService.getByPublicName(publicName);
            if (!chatEntityOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Chat with publicName '" + publicName + "' not found");
            }
            var chatEntity = chatEntityOpt.get();
            if (request.getModel() == null || request.getModel().isBlank()) {
                request.setModel(defaultModel);
            }
            boolean rag = (useRag == null) ? true : useRag;
            log.info("POST /v1/chat/completions stream={} useRag={} model={} msgs={}",
                request.isStream(), rag, request.getModel(),
                (request.getMessages() == null ? 0 : request.getMessages().size()));

            if (request.isStream()) {
                // Return SSE streaming response
                SseEmitter emitter = new SseEmitter(30000L); // 30 second timeout
                CompletableFuture.runAsync(() -> {
                    try {
                        chatService.chatStream(request, rag, chatEntity)
                            .doOnNext(chunk -> {
                                try {
                                    String jsonData = toJson(chunk);
                                    emitter.send(SseEmitter.event()
                                        .data(jsonData));
                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            })
                            .doOnComplete(() -> {
                                try {
                                    emitter.send(SseEmitter.event().data("[DONE]"));
                                    emitter.complete();
                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            })
                            .doOnError(emitter::completeWithError)
                            .subscribe();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                });
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(emitter);
            } else {
                // Non-streaming response
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(chatService.chat(request, rag, chatEntity));
            }
        } catch (Throwable t) {
            log.error("Error in createCompletion", t);
            return ResponseEntity.status(500).body("Internal server error: " + t.getMessage());
        }
    }

    private static final ObjectMapper mapper = new ObjectMapper();
    
    private static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialization_failed\"}";
        }
    }
}

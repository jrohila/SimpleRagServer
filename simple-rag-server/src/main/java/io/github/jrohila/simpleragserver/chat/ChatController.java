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
            
            // Apply LLMConfig from ChatEntity - server config always overrides client values
            applyLLMConfigToRequest(request, chatEntity.getLlmConfig());
            
            if (request.getModel() == null || request.getModel().isBlank()) {
                request.setModel(defaultModel);
            }
            boolean rag = (useRag == null) ? true : useRag;
            log.info("POST /v1/chat/completions stream={} useRag={} model={} msgs={} maxTokens={} temp={} topP={} topK={} freqPenalty={}",
                request.isStream(), rag, request.getModel(),
                (request.getMessages() == null ? 0 : request.getMessages().size()),
                request.getMaxTokens(), request.getTemperature(), 
                request.getTopP(), request.getTopK(), request.getFrequencyPenalty());

            if (request.isStream()) {
                // Return SSE streaming response with 5 minute timeout
                SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout (300000 ms)
                
                // Add timeout handler
                emitter.onTimeout(() -> {
                    log.warn("SSE connection timed out for chat: {}", publicName);
                    try {
                        emitter.send(SseEmitter.event()
                            .data("{\"error\":\"Request timed out. Please try a shorter query or increase timeout.\"}"));
                        emitter.complete();
                    } catch (IOException | IllegalStateException e) {
                        log.error("Error sending timeout message", e);
                    }
                });
                
                // Add error handler
                emitter.onError((ex) -> {
                    log.error("Error in SSE stream", ex);
                    emitter.completeWithError(ex);
                });
                
                // Add completion handler
                emitter.onCompletion(() -> {
                    log.info("SSE stream completed for chat: {}", publicName);
                });
                
                CompletableFuture.runAsync(() -> {
                    try {
                        chatService.chatStream(request, chatEntity)
                            .doOnNext(chunk -> {
                                try {
                                    String jsonData = toJson(chunk);
                                    emitter.send(SseEmitter.event()
                                        .data(jsonData));
                                } catch (IOException e) {
                                    log.error("Error sending SSE message", e);
                                    emitter.completeWithError(e);
                                } catch (IllegalStateException e) {
                                    log.warn("Attempted to send to completed emitter", e);
                                    // Silently ignore - connection already closed
                                }
                            })
                            .doOnComplete(() -> {
                                try {
                                    emitter.send(SseEmitter.event().data("[DONE]"));
                                    emitter.complete();
                                } catch (IOException | IllegalStateException e) {
                                    log.debug("Error completing emitter", e);
                                }
                            })
                            .doOnError((error) -> {
                                log.error("Error in chat completion stream", error);
                                try {
                                    emitter.send(SseEmitter.event()
                                        .data("{\"error\":\"" + error.getMessage() + "\"}"));
                                    emitter.completeWithError(error);
                                } catch (IOException | IllegalStateException e) {
                                    log.error("Error sending error message", e);
                                }
                            })
                            .subscribe();
                    } catch (Exception e) {
                        log.error("Exception in async task", e);
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
                    .body(chatService.chat(request, chatEntity));
            }
        } catch (Throwable t) {
            log.error("Error in createCompletion", t);
            return ResponseEntity.status(500).body("Internal server error: " + t.getMessage());
        }
    }

    /**
     * Apply LLMConfig values from ChatEntity to OpenAiChatRequest.
     * Server-side configuration ALWAYS overrides client values for security and consistency.
     */
    private void applyLLMConfigToRequest(OpenAiChatRequest request, io.github.jrohila.simpleragserver.domain.LLMConfig llmConfig) {
        if (llmConfig == null) {
            log.warn("No LLMConfig found, using request defaults");
            return;
        }
        
        // Always override with server-side config - don't trust client values
        if (llmConfig.getMaxNewTokens() != null) {
            request.setMaxTokens(llmConfig.getMaxNewTokens());
        }
        
        if (llmConfig.getTemperature() != null) {
            request.setTemperature(llmConfig.getTemperature());
        }
        
        if (llmConfig.getTopP() != null) {
            request.setTopP(llmConfig.getTopP());
        }
        
        if (llmConfig.getTopK() != null) {
            request.setTopK(llmConfig.getTopK());
        }
        
        if (llmConfig.getRepetitionPenalty() != null) {
            request.setFrequencyPenalty(llmConfig.getRepetitionPenalty());
        }
        
        if (llmConfig.getMinNewTokens() != null) {
            request.setMinTokens(llmConfig.getMinNewTokens());
        }
        
        if (llmConfig.getDoSample() != null) {
            request.setDoSample(llmConfig.getDoSample());
        }
        
        log.debug("Applied server-side LLMConfig (overriding client) - maxTokens: {}, temp: {}, topP: {}, topK: {}, freqPenalty: {}, minTokens: {}, doSample: {}",
            request.getMaxTokens(), request.getTemperature(), request.getTopP(), 
            request.getTopK(), request.getFrequencyPenalty(), request.getMinTokens(), request.getDoSample());
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

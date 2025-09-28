package io.github.jrohila.simpleragserver.chat;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // OpenAI-compatible chat completions endpoint
    @PostMapping(path = {"/v1/chat/completions", "/api/chat"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createCompletion(@RequestBody OpenAiChatRequest request,
                                             @RequestParam(value = "useRag", required = false) Boolean useRag) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            request.setModel("tinyllama:1.1b");
        }
        boolean rag = (useRag == null) ? true : useRag;
        if (request.isStream()) {
            // SSE style: each element serialized as JSON line; client terminates on finishReason=stop
            Flux<OpenAiChatStreamChunk> flux = chatService.chatStream(request);
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(flux.map(chunk -> "data: " + toJson(chunk) + "\n\n"));
        }
        return ResponseEntity.ok(chatService.chat(request, rag));
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialization_failed\"}";
        }
    }
}

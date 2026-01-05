package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.service.ChatService;
import io.github.jrohila.simpleragserver.dto.OpenAiChatRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.MediaType;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.QueryValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jrohila.simpleragserver.repository.ChatManagerService;
import java.io.IOException;
import reactor.core.publisher.Flux;

@Controller
public class ChatController {

    private final ChatManagerService chatManagerService;

    private final ChatService chatService;
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Value("${llm.ollama.model}")
    private String defaultModel;

    public ChatController(ChatService chatService, ChatManagerService chatManagerService) {
        this.chatService = chatService;
        this.chatManagerService = chatManagerService;
    }

    // OpenAI-compatible chat completions endpoint
    @Post("/{publicName}/v1/chat/completions")
    public HttpResponse<?> createCompletion(@PathVariable String publicName,
                                            @Body OpenAiChatRequestDTO request,
                                            @QueryValue(value = "useRag", defaultValue = "true") boolean useRag) {
        try {
            // Fetch ChatEntity by publicName
            var chatEntityOpt = chatManagerService.getByPublicName(publicName);
            if (chatEntityOpt.isEmpty()) {
                return HttpResponse.badRequest("Chat with publicName '" + publicName + "' not found");
            }
            var chatEntity = chatEntityOpt.get();

            // Apply LLMConfig from ChatEntity - server config always overrides client values
            applyLLMConfigToRequest(request, chatEntity.getLlmConfig());

            if (request.getModel() == null || request.getModel().isBlank()) {
                request.setModel(defaultModel);
            }
            boolean rag = useRag;
            log.info("POST /v1/chat/completions stream={} useRag={} model={} msgs={} maxTokens={} temp={} topP={} topK={} freqPenalty={}",
                    request.isStream(), rag, request.getModel(),
                    (request.getMessages() == null ? 0 : request.getMessages().size()),
                    request.getMaxTokens(), request.getTemperature(),
                    request.getTopP(), request.getTopK(), request.getFrequencyPenalty());

            if (request.isStream()) {
                // Return Reactor Flux as SSE stream
                Flux<?> flux = chatService.chatStream(request, chatEntity);
                return HttpResponse.ok(flux).contentType(MediaType.TEXT_EVENT_STREAM_TYPE);
            } else {
                // Non-streaming response
                return HttpResponse.ok(chatService.chat(request, chatEntity)).contentType(MediaType.APPLICATION_JSON_TYPE);
            }
        } catch (Throwable t) {
            log.error("Error in createCompletion", t);
            return HttpResponse.serverError("Internal server error: " + t.getMessage());
        }
    }

    /**
     * Apply LLMConfig values from ChatEntity to OpenAiChatRequest. Server-side
     * configuration ALWAYS overrides client values for security and
     * consistency.
     */
    private void applyLLMConfigToRequest(OpenAiChatRequestDTO request, io.github.jrohila.simpleragserver.domain.LLMConfig llmConfig) {
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

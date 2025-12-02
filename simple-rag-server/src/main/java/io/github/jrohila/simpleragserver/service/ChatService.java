package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.dto.OpenAiChatRequestDTO;
import io.github.jrohila.simpleragserver.dto.OpenAiChatResponseDTO;
import io.github.jrohila.simpleragserver.dto.OpenAiChatStreamChunkDTO;
import io.github.jrohila.simpleragserver.util.TokenGenerator;
import io.github.jrohila.simpleragserver.domain.ChatEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// removed unused jtokkit imports (token counting handled via ChatHelper)

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import io.github.jrohila.simpleragserver.client.LlmClient;
import io.github.jrohila.simpleragserver.client.LlmClientFactory;
import io.github.jrohila.simpleragserver.client.LlmRequestOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional; // added
// Add detector import
import io.github.jrohila.simpleragnlp.TitleRequestDetector; // assumes detector is in this package
import io.github.jrohila.simpleragserver.pipeline.ContextAdditionPipe;
import io.github.jrohila.simpleragserver.pipeline.ContextAdditionPipe.OperationResult;
import io.github.jrohila.simpleragserver.util.ChatHelper;
import io.github.jrohila.simpleragserver.dto.MessageDTO;
import io.github.jrohila.simpleragserver.repository.ChunkSearchService;
import org.apache.commons.lang3.tuple.Pair;

@Service
public class ChatService {

    public static enum ChatProcessResult {
        MESSAGES_HANDLED, PROMPT_OUT_OF_SCOPE
    }

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ContextAdditionPipe contextAdditionPipe;

    @Autowired
    private ChatHelper chatHelper;

    private final LlmClientFactory llmClientFactory;
    private final TitleRequestDetector titleRequestDetector; // added

    @Autowired
    private ChatStreamConsumer streamConsumer; // optional hook for streaming capture

    @Autowired
    private ChatResponsePostProcessor postProcessor;

    @Autowired
    public ChatService(LlmClientFactory llmClientFactory, ChunkSearchService chunkSearchService,
            // detector is optional to avoid failing if bean not present
            Optional<TitleRequestDetector> titleRequestDetector) {
        this.llmClientFactory = llmClientFactory;
        this.titleRequestDetector = titleRequestDetector != null ? titleRequestDetector.orElse(null) : null;
    }

    private Pair<ChatProcessResult, List<MessageDTO>> handleMessage(OpenAiChatRequestDTO request, ChatEntity chatEntity) {
        ChatProcessResult result = ChatProcessResult.MESSAGES_HANDLED;

        String firstUserContent = null;
        if (request.getMessages() != null) {
            for (MessageDTO m : request.getMessages()) {
                if (MessageDTO.Role.USER.equals(m.getRole())) {
                    firstUserContent = m.getContentAsString();
                    break;
                }
            }
        }
        boolean isTitleRequest = firstUserContent != null
                && titleRequestDetector != null
                && titleRequestDetector.isTitleRequest(firstUserContent);

        List<MessageDTO> springMessages = new ArrayList<>();
        if (isTitleRequest) {
            if (log.isDebugEnabled()) {
                log.debug("[ChatService] Title request detected (stream); sending only first user message to LLM (no systemAppend, no RAG).");
            }
            List<MessageDTO> only = new ArrayList<>();
            only.add(new MessageDTO(MessageDTO.Role.USER, firstUserContent));
            springMessages = only;
            if (log.isDebugEnabled()) {
                log.debug("[ChatService] Streaming: sending to LLM ({} messages):", only.size());
                log.debug("  #0 [UserMessage] {}", firstUserContent);
            }
        } else {
            log.info("[ChatService] chatStream invoked: msgs={} model={} ", (request.getMessages() == null ? 0 : request.getMessages().size()), request.getModel());
            List<Integer> rollingTokens = TokenGenerator.createTokens(springMessages);

            Pair<OperationResult, List<MessageDTO>> contextResult = this.contextAdditionPipe.process(request.getMessages(), chatEntity);
            if (OperationResult.CONTEXT_ADDED.equals(contextResult.getKey())) {
                springMessages = this.contextAdditionPipe.appendMemory(request.getMessages(), rollingTokens, chatEntity);
            } else {
                springMessages = this.contextAdditionPipe.appendMemory(request.getMessages(), rollingTokens, chatEntity);
                result = ChatProcessResult.PROMPT_OUT_OF_SCOPE;
            }

            // DEBUG: log exactly what will be sent to the LLM (streaming)
            if (log.isDebugEnabled()) {
                log.debug("[ChatService] Streaming: sending to LLM ({} messages):", springMessages.size());
                int i = 0;
                for (MessageDTO msg : springMessages) {
                    log.debug("  #{} [{}] {}", i++, msg.getClass().getSimpleName(), msg.getContentAsString());
                }
            }
        }
        return Pair.of(result, springMessages);
    }

    public OpenAiChatResponseDTO chat(OpenAiChatRequestDTO request, ChatEntity chatEntity) {
        // Detect title request from the first user message, and short-circuit
        Pair<ChatProcessResult, List<MessageDTO>> processResult = this.handleMessage(request, chatEntity);
        if (ChatProcessResult.PROMPT_OUT_OF_SCOPE.equals(processResult.getKey())) {
            String outOfScopeMsg = chatEntity.getDefaultOutOfScopeMessage();
            log.info("[ChatService] Prompt out of scope. Returning default out-of-scope message: {}", outOfScopeMsg);
            OpenAiChatResponseDTO out = new OpenAiChatResponseDTO();
            out.setId("chatcmpl-" + java.util.UUID.randomUUID());
            out.setModel(request.getModel());
            OpenAiChatResponseDTO.Choice choice = new OpenAiChatResponseDTO.Choice();
            choice.setIndex(0);
            choice.setFinishReason("stop");
            choice.setMessage(new MessageDTO("assistant", outOfScopeMsg));
            out.setChoices(java.util.List.of(choice));
            OpenAiChatResponseDTO.Usage usage = new OpenAiChatResponseDTO.Usage();
            usage.setPromptTokens(0);
            usage.setCompletionTokens(outOfScopeMsg != null ? outOfScopeMsg.length() : 0);
            usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
            out.setUsage(usage);
            return out;
        } else {
            List<MessageDTO> springMessages = processResult.getValue();

            // Log prompt token length using jtokkit
            try {
                int promptTokens = this.chatHelper.countTokensForMessages(springMessages);
                log.info("[ChatService] Tokens (prompt via jtokkit/CL100K): {} tokens across {} messages", promptTokens, springMessages.size());
            } catch (Exception e) {
                log.debug("[ChatService] Token count (prompt) failed: {}", e.getMessage());
            }

            // Convert MessageDTO to langchain4j ChatMessage
            List<ChatMessage> chatMessages = convertToChatMessages(springMessages);
            
            // Build request options
            LlmRequestOptions options = buildLlmRequestOptions(request);
            
            // Get LLM client and make the call
            LlmClient client = llmClientFactory.getDefaultClient();
            Response<String> resp = client.chat(chatMessages, options);

            String assistantContent = resp != null && resp.content() != null ? resp.content() : "";

            // Log completion token length using jtokkit
            int completionTokensCount = 0;
            try {
                completionTokensCount = this.chatHelper.countTokens(assistantContent);
                log.info("[ChatService] Tokens (completion via jtokkit/CL100K): {} tokens", completionTokensCount);
            } catch (Exception e) {
                log.debug("[ChatService] Token count (completion) failed: {}", e.getMessage());
            }

            OpenAiChatResponseDTO out = new OpenAiChatResponseDTO();
            out.setId("chatcmpl-" + UUID.randomUUID());
            out.setModel(request.getModel());

            OpenAiChatResponseDTO.Choice choice = new OpenAiChatResponseDTO.Choice();
            choice.setIndex(0);
            choice.setFinishReason("stop");
            choice.setMessage(new MessageDTO("assistant", assistantContent));
            out.setChoices(List.of(choice));

            // Usage counts using jtokkit
            OpenAiChatResponseDTO.Usage usage = new OpenAiChatResponseDTO.Usage();
            int promptTokens = 0;
            try {
                promptTokens = this.chatHelper.countTokensForMessages(springMessages);
            } catch (Exception ignore) {
            }
            usage.setPromptTokens(promptTokens);
            usage.setCompletionTokens(completionTokensCount);
            usage.setTotalTokens(promptTokens + completionTokensCount);
            out.setUsage(usage);
            return out;
        }
    }

    /**
     * Streaming version returning a Flux of OpenAI-compatible streaming chunks.
     */
    public Flux<OpenAiChatStreamChunkDTO> chatStream(OpenAiChatRequestDTO request, ChatEntity chatEntity) {
        // Detect title request from the first user message, and short-circuit
        Pair<ChatProcessResult, List<MessageDTO>> processResult = this.handleMessage(request, chatEntity);
        if (ChatProcessResult.PROMPT_OUT_OF_SCOPE.equals(processResult.getKey())) {
            String outOfScopeMsg = chatEntity.getDefaultOutOfScopeMessage();
            log.info("[ChatService] Prompt out of scope (stream). Returning default out-of-scope message: {}", outOfScopeMsg);
            String id = "chatcmpl-" + java.util.UUID.randomUUID();
            String model = request.getModel();
            OpenAiChatStreamChunkDTO chunk = new OpenAiChatStreamChunkDTO();
            chunk.setId(id);
            chunk.setModel(model);
            OpenAiChatStreamChunkDTO.ChoiceDelta choice = new OpenAiChatStreamChunkDTO.ChoiceDelta();
            choice.setIndex(0);
            choice.setFinishReason("stop");
            OpenAiChatStreamChunkDTO.Delta delta = new OpenAiChatStreamChunkDTO.Delta();
            delta.setRole("assistant");
            delta.setContent(outOfScopeMsg);
            choice.setDelta(delta);
            chunk.setChoices(java.util.List.of(choice));
            return Flux.just(chunk);
        } else {
            List<MessageDTO> springMessages = processResult.getValue();

            // Compute and log rolling tokens (based on newest-first last up to 5 USER messages only)
            List<Integer> rollingTokens = new ArrayList<>();
            try {
                rollingTokens = TokenGenerator.createTokens(springMessages);
                log.info("[ChatService] Rolling tokens (user-only, newest-first, streaming, size={}): {}", rollingTokens.size(), rollingTokens);
            } catch (Exception e) {
                log.debug("[ChatService] Rolling token computation (streaming) failed: {}", e.getMessage());
            }

            String id = "chatcmpl-" + UUID.randomUUID();

            postProcessor.addContext(id, springMessages, rollingTokens);

            // Log prompt token length using jtokkit
            try {
                int promptTokens = this.chatHelper.countTokensForMessages(springMessages);
                log.info("[ChatService] Tokens (prompt via jtokkit/CL100K, streaming): {} tokens across {} messages", promptTokens, springMessages.size());
            } catch (Exception e) {
                log.debug("[ChatService] Token count (prompt, streaming) failed: {}", e.getMessage());
            }
            String model = request.getModel();
            AtomicBoolean first = new AtomicBoolean(true);
            StringBuilder cumulative = new StringBuilder();
            AtomicInteger index = new AtomicInteger(0);

            // Convert MessageDTO to langchain4j ChatMessage
            List<ChatMessage> chatMessages = convertToChatMessages(springMessages);
            
            // Build request options
            LlmRequestOptions options = buildLlmRequestOptions(request);
            
            // Get LLM client
            LlmClient client = llmClientFactory.getDefaultClient();
            
            return Flux.<OpenAiChatStreamChunkDTO>create(sink -> {
                log.info("[ChatService] Creating Flux for streaming response");
                client.streamChat(chatMessages, options, 
                    // Token handler
                    token -> {
                        log.debug("[ChatService] Received token from LlmClient: '{}'", token);
                        cumulative.append(token);
                        // Optional capture of per-delta content
                        try {
                            if (streamConsumer != null) {
                                streamConsumer.onDelta(id, token);
                            }
                        } catch (Exception ignore) {
                        }
                        
                        OpenAiChatStreamChunkDTO chunk = new OpenAiChatStreamChunkDTO();
                        chunk.setId(id);
                        chunk.setModel(model);
                        OpenAiChatStreamChunkDTO.ChoiceDelta choice = new OpenAiChatStreamChunkDTO.ChoiceDelta();
                        choice.setIndex(index.get());
                        OpenAiChatStreamChunkDTO.Delta delta = new OpenAiChatStreamChunkDTO.Delta();
                        if (first.getAndSet(false)) {
                            delta.setRole("assistant");
                        }
                        delta.setContent(token);
                        choice.setDelta(delta);
                        chunk.setChoices(List.of(choice));
                        log.debug("[ChatService] Emitting chunk to Flux sink");
                        sink.next(chunk);
                    },
                    // Completion handler
                    () -> {
                        log.info("[ChatService] Stream completed, calling sink.complete()");
                        sink.complete();
                    },
                    // Error handler
                    error -> {
                        log.error("[ChatService] Stream error, calling sink.error()", error);
                        sink.error(error);
                    }
                );
            })
                    .concatWith(Mono.fromSupplier(() -> {
                        // On stream completion, log completion tokens using accumulated content
                        try {
                            int completionTokens = this.chatHelper.countTokens(cumulative.toString());
                            log.info("[ChatService] Tokens (completion via jtokkit/CL100K, streaming): {} tokens", completionTokens);
                        } catch (Exception e) {
                            log.debug("[ChatService] Token count (completion, streaming) failed: {}", e.getMessage());
                        }
                        // Optional capture of full response
                        try {
                            if (streamConsumer != null) {
                                streamConsumer.onComplete(id, cumulative.toString());
                            }
                        } catch (Exception ignore) {
                        }
                        OpenAiChatStreamChunkDTO done = new OpenAiChatStreamChunkDTO();
                        done.setId(id);
                        done.setModel(model);
                        OpenAiChatStreamChunkDTO.ChoiceDelta choice = new OpenAiChatStreamChunkDTO.ChoiceDelta();
                        choice.setIndex(index.get());
                        choice.setFinishReason("stop");
                        OpenAiChatStreamChunkDTO.Delta delta = new OpenAiChatStreamChunkDTO.Delta();
                        delta.setContent("");
                        choice.setDelta(delta);
                        done.setChoices(List.of(choice));
                        return done;
                    }));
        }
    }

    /**
     * Build LlmRequestOptions from OpenAiChatRequest parameters.
     * Note: model parameter from request is ignored - client uses its configured default model.
     */
    private LlmRequestOptions buildLlmRequestOptions(OpenAiChatRequestDTO request) {
        return LlmRequestOptions.builder()
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .topP(request.getTopP())
                .topK(request.getTopK())
                .frequencyPenalty(request.getFrequencyPenalty())
                .build();
    }
    
    /**
     * Convert MessageDTO list to langchain4j ChatMessage list.
     */
    private List<ChatMessage> convertToChatMessages(List<MessageDTO> messageDTOs) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (MessageDTO dto : messageDTOs) {
            String content = dto.getContentAsString();
            if (content == null) continue;
            
            ChatMessage message = switch (dto.getRole()) {
                case SYSTEM -> new SystemMessage(content);
                case USER -> new UserMessage(content);
                case ASSISTANT -> new AiMessage(content);
                case TOOL -> new UserMessage(content); // Map tool to user for now
            };
            chatMessages.add(message);
        }
        return chatMessages;
    }
}

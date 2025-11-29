package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.dto.OpenAiChatRequestDTO;
import io.github.jrohila.simpleragserver.dto.OpenAiChatResponseDTO;
import io.github.jrohila.simpleragserver.dto.OpenAiChatStreamChunkDTO;
import io.github.jrohila.simpleragserver.chat.util.TokenGenerator;
import io.github.jrohila.simpleragserver.domain.ChatEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// removed unused jtokkit imports (token counting handled via ChatHelper)

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.api.OllamaOptions;
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
import io.github.jrohila.simpleragserver.chat.pipeline.ContextAdditionPipe;
import io.github.jrohila.simpleragserver.chat.pipeline.ContextAdditionPipe.OperationResult;
import io.github.jrohila.simpleragserver.chat.pipeline.MessageListPreProcessPipe;
import io.github.jrohila.simpleragserver.chat.util.ChatHelper;
import io.github.jrohila.simpleragserver.chat.util.GraniteHelper;
import io.github.jrohila.simpleragserver.dto.MessageDTO;
import io.github.jrohila.simpleragserver.service.ChatResponsePostProcessor;
import io.github.jrohila.simpleragserver.repository.ChunkSearchService;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.lang3.tuple.Pair;

@Service
public class ChatService {

    public static enum ChatProcessResult {
        MESSAGES_HANDLED, PROMPT_OUT_OF_SCOPE
    }

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private MessageListPreProcessPipe messageListPreProcessPipe;

    @Autowired
    private ContextAdditionPipe contextAdditionPipe;

    @Autowired
    private ChatHelper chatHelper;

    private final ChatModel chatModel;
    private final TitleRequestDetector titleRequestDetector; // added

    @Autowired
    private ChatStreamConsumer streamConsumer; // optional hook for streaming capture

    @Autowired
    private ChatResponsePostProcessor postProcessor;

    @Autowired
    public ChatService(ChatModel chatModel, ChunkSearchService chunkSearchService,
            // detector is optional to avoid failing if bean not present
            Optional<TitleRequestDetector> titleRequestDetector) {
        this.chatModel = chatModel;
        this.titleRequestDetector = titleRequestDetector != null ? titleRequestDetector.orElse(null) : null;
    }

    private Pair<ChatProcessResult, List<Message>> handleMessage(OpenAiChatRequestDTO request, ChatEntity chatEntity) {
        ChatProcessResult result = ChatProcessResult.MESSAGES_HANDLED;

        String firstUserContent = null;
        if (request.getMessages() != null) {
            for (MessageDTO m : request.getMessages()) {
                if ("user".equals(m.getRole())) {
                    firstUserContent = m.getContentAsString();
                    break;
                }
            }
        }
        boolean isTitleRequest = firstUserContent != null
                && titleRequestDetector != null
                && titleRequestDetector.isTitleRequest(firstUserContent);

        List<Message> springMessages = new ArrayList<>();
        if (isTitleRequest) {
            if (log.isDebugEnabled()) {
                log.debug("[ChatService] Title request detected (stream); sending only first user message to LLM (no systemAppend, no RAG).");
            }
            List<Message> only = new ArrayList<>();
            only.add(new UserMessage(firstUserContent));
            springMessages = only;
            if (log.isDebugEnabled()) {
                log.debug("[ChatService] Streaming: sending to LLM ({} messages):", only.size());
                log.debug("  #0 [UserMessage] {}", firstUserContent);
            }
        } else {
            log.info("[ChatService] chatStream invoked: msgs={} model={} ", (request.getMessages() == null ? 0 : request.getMessages().size()), request.getModel());
            List<Integer> rollingTokens = TokenGenerator.createTokens(springMessages);

            springMessages = this.messageListPreProcessPipe.transform(request);
            Pair<OperationResult, List<Message>> contextResult = this.contextAdditionPipe.process(springMessages, chatEntity);
            if (OperationResult.CONTEXT_ADDED.equals(contextResult.getKey())) {
                springMessages = this.contextAdditionPipe.appendMemory(springMessages, rollingTokens, chatEntity);
            } else {
                springMessages = this.contextAdditionPipe.appendMemory(springMessages, rollingTokens, chatEntity);
                result = ChatProcessResult.PROMPT_OUT_OF_SCOPE;
            }

            // DEBUG: log exactly what will be sent to the LLM (streaming)
            if (log.isDebugEnabled()) {
                log.debug("[ChatService] Streaming: sending to LLM ({} messages):", springMessages.size());
                int i = 0;
                for (Message msg : springMessages) {
                    log.debug("  #{} [{}] {}", i++, msg.getClass().getSimpleName(), this.chatHelper.extractMessageText(msg));
                }
            }
        }
        return Pair.of(result, springMessages);
    }

    public OpenAiChatResponseDTO chat(OpenAiChatRequestDTO request, ChatEntity chatEntity) {
        // Detect title request from the first user message, and short-circuit
        Pair<ChatProcessResult, List<Message>> processResult = this.handleMessage(request, chatEntity);
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
            List<Message> springMessages = processResult.getValue();

            Prompt prompt = GraniteHelper.toGranitePrompt(springMessages, buildOllamaOptions(request));
            // Log prompt token length using jtokkit
            try {
                int promptTokens = this.chatHelper.countTokensForMessages(springMessages);
                log.info("[ChatService] Tokens (prompt via jtokkit/CL100K): {} tokens across {} messages", promptTokens, springMessages.size());
            } catch (Exception e) {
                log.debug("[ChatService] Token count (prompt) failed: {}", e.getMessage());
            }

            ChatResponse resp = chatModel.call(prompt);

            String assistantContent = "";
            if (resp != null && resp.getResults() != null && !resp.getResults().isEmpty()) {
                var outMsg = resp.getResults().get(0).getOutput();
                if (outMsg instanceof AssistantMessage am) {
                    assistantContent = am.getText();
                } else if (outMsg != null) {
                    // fallback: attempt reflective text/content retrieval, else toString
                    try {
                        var mText = outMsg.getClass().getMethod("getText");
                        Object v = mText.invoke(outMsg);
                        assistantContent = v != null ? v.toString() : "";
                    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e1) {
                        try {
                            var mContent = outMsg.getClass().getMethod("getContent");
                            Object v2 = mContent.invoke(outMsg);
                            assistantContent = v2 != null ? v2.toString() : "";
                        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e2) {
                            assistantContent = outMsg.toString();
                        }
                    }
                }
            }

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
        Pair<ChatProcessResult, List<Message>> processResult = this.handleMessage(request, chatEntity);
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
            List<Message> springMessages = processResult.getValue();

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

            Prompt granitePrompt = GraniteHelper.toGranitePrompt(springMessages, buildOllamaOptions(request));
            return chatModel.stream(granitePrompt)
                    .flatMap(resp -> Flux.fromIterable(resp.getResults()))
                    .map(result -> {
                        String assistantContent = "";
                        var outMsg = result.getOutput();
                        if (outMsg instanceof AssistantMessage am) {
                            try {
                                assistantContent = am.getText();
                            } catch (Exception ignored) {
                                assistantContent = am.toString();
                            }
                        }
                        // Determine delta vs previous cumulative
                        String newDelta;
                        if (assistantContent.startsWith(cumulative.toString())) {
                            newDelta = assistantContent.substring(cumulative.length());
                        } else {
                            // fallback (model may already send only delta)
                            newDelta = assistantContent;
                        }
                        if (!newDelta.isEmpty()) {
                            cumulative.append(newDelta);
                            // Optional capture of per-delta content
                            try {
                                if (streamConsumer != null) {
                                    streamConsumer.onDelta(id, newDelta);
                                }
                            } catch (Exception ignore) {
                            }
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
                        delta.setContent(newDelta);
                        choice.setDelta(delta);
                        chunk.setChoices(List.of(choice));
                        return chunk;
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
     * Build OllamaOptions from OpenAiChatRequest parameters. Maps all available
     * LLM parameters to Ollama-specific options. Supports both standard OpenAI
     * parameters and extended Ollama parameters.
     */
    private OllamaOptions buildOllamaOptions(OpenAiChatRequestDTO request) {
        OllamaOptions.Builder builder = OllamaOptions.builder();

        // Core generation parameters
        if (request.getTemperature() != null) {
            builder.temperature(request.getTemperature());
        }
        if (request.getTopP() != null) {
            builder.topP(request.getTopP());
        }
        if (request.getTopK() != null) {
            builder.topK(request.getTopK());
        }

        // Token limits
        if (request.getMaxTokens() != null) {
            builder.numPredict(request.getMaxTokens());
        }

        // Penalty parameters (control repetition)
        if (request.getFrequencyPenalty() != null) {
            builder.frequencyPenalty(request.getFrequencyPenalty());
        }

        // Additional Ollama-specific parameters that can be added:
        // - repeatPenalty: penalize repetitions (similar to frequencyPenalty but Ollama-native)
        // - presencePenalty: penalize tokens based on presence in context
        // - stop: stop sequences
        // - seed: for reproducible generation
        // - numCtx: context window size
        // - numBatch: batch size for prompt evaluation
        // - numGpu: number of layers to offload to GPU
        // - mainGpu: primary GPU to use
        // - lowVram: reduce VRAM usage
        // - f16Kv: use fp16 for key/value cache
        // - logitsAll: return logits for all tokens
        // - vocabOnly: only load vocabulary
        // - useMmap: use memory mapping
        // - useMlock: lock model in memory
        // - embeddingOnly: only return embeddings
        // - ropeFrequencyBase: RoPE frequency base
        // - ropeFrequencyScale: RoPE frequency scale
        // - numThread: number of threads to use
        // Note: minTokens and doSample are not directly supported by OllamaOptions
        // minTokens could be handled by post-processing or custom prompting
        // doSample is implicitly controlled by temperature (0 = greedy, >0 = sampling)
        log.debug("Built OllamaOptions: temp={}, topP={}, topK={}, maxTokens={}, freqPenalty={}",
                request.getTemperature(), request.getTopP(), request.getTopK(),
                request.getMaxTokens(), request.getFrequencyPenalty());

        return builder.build();
    }
}

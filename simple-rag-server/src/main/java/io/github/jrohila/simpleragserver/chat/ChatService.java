package io.github.jrohila.simpleragserver.chat;

import io.github.jrohila.simpleragserver.chat.util.TokenGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// removed unused jtokkit imports (token counting handled via ChatHelper)

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
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
import io.github.jrohila.simpleragserver.chat.util.ChatHelper;
import io.github.jrohila.simpleragserver.chat.util.GraniteHelper;
import io.github.jrohila.simpleragserver.service.ChatResponsePostProcessor;
import io.github.jrohila.simpleragserver.repository.ChunkSearchService;
import java.lang.reflect.InvocationTargetException;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ContextAdditionPipe contextAdditionPipe;

    @Autowired
    private ChatHelper chatHelper;

    private final ChatModel chatModel;
    private final String systemAppend;
    private final TitleRequestDetector titleRequestDetector; // added

    @Autowired
    private ChatStreamConsumer streamConsumer; // optional hook for streaming capture

    @Autowired
    private ChatResponsePostProcessor postProcessor;

    @Autowired
    public ChatService(ChatModel chatModel, ChunkSearchService chunkSearchService,
            // detector is optional to avoid failing if bean not present
            Optional<TitleRequestDetector> titleRequestDetector,
            @Value("${processing.chat.system.append}") String systemAppend) {
        this.chatModel = chatModel;
        this.systemAppend = systemAppend;
        this.titleRequestDetector = titleRequestDetector != null ? titleRequestDetector.orElse(null) : null;
    }

    public OpenAiChatResponse chat(OpenAiChatRequest request) {
        return chat(request, true);
    }

    private List<Message> handleMessage(OpenAiChatRequest request, boolean useRag) {
        String firstUserContent = null;
        if (request.getMessages() != null) {
            for (OpenAiChatRequest.Message m : request.getMessages()) {
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
            boolean hasClientSystemOrAssistantStream = false;
            log.info("[ChatService] chatStream invoked: msgs={} model={} ",
                    (request.getMessages() == null ? 0 : request.getMessages().size()),
                    request.getModel());
            if (request.getMessages() != null) {
                for (OpenAiChatRequest.Message m : request.getMessages()) {
                    if (m.getRole() == null) {
                        continue;
                    }
                    switch (m.getRole()) {
                        case "system" -> {
                            String content = m.getContentAsString();
                            if (content == null) {
                                content = "";
                            }
                            if (systemAppend != null && !systemAppend.isBlank()) {
                                String trimmedAppend = systemAppend.trim();
                                String trimmedContent = content.trim();
                                if (!trimmedContent.endsWith(trimmedAppend)) {
                                    content = (trimmedContent.isEmpty() ? trimmedAppend : trimmedContent + "\n" + trimmedAppend);
                                }
                            }
                            springMessages.add(new SystemMessage(content));
                            hasClientSystemOrAssistantStream = true;
                        }
                        case "user" ->
                            springMessages.add(new UserMessage(m.getContentAsString()));
                        case "assistant" -> {
                            // Do NOT append system text to assistant messages in streaming path.
                            springMessages.add(new AssistantMessage(m.getContentAsString()));
                            hasClientSystemOrAssistantStream = true;
                        }
                        default ->
                            springMessages.add(new UserMessage(m.getContentAsString()));
                    }
                }
            }
            if (!hasClientSystemOrAssistantStream && systemAppend != null && !systemAppend.isBlank()) {
                springMessages.add(0, new SystemMessage(systemAppend));
            }

            // Compute and log rolling tokens (based on newest-first last up to 5 USER messages only)
            List<Integer> rollingTokens = null;
            try {
                rollingTokens = TokenGenerator.createTokens(springMessages);
                log.info("[ChatService] Rolling tokens (user-only, newest-first, size={}): {}", rollingTokens.size(), rollingTokens);
            } catch (Exception e) {
                log.debug("[ChatService] Rolling token computation failed: {}", e.getMessage());
            }

            // Build RAG context
            if (useRag) {
                springMessages = contextAdditionPipe.process(springMessages);
            }
            
            if ((rollingTokens != null) && (!rollingTokens.isEmpty())) {
                springMessages = contextAdditionPipe.appendMemory(springMessages, rollingTokens);
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
        return springMessages;
    }

    public OpenAiChatResponse chat(OpenAiChatRequest request, boolean useRag) {
        // Detect title request from the first user message, and short-circuit
        List<Message> springMessages = this.handleMessage(request, useRag);

        Prompt prompt = GraniteHelper.toGranitePrompt(springMessages);        
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

        OpenAiChatResponse out = new OpenAiChatResponse();
        out.setId("chatcmpl-" + UUID.randomUUID());
        out.setModel(request.getModel());

        OpenAiChatResponse.Choice choice = new OpenAiChatResponse.Choice();
        choice.setIndex(0);
        choice.setFinishReason("stop");
        choice.setMessage(new OpenAiChatRequest.Message("assistant", assistantContent));
        out.setChoices(List.of(choice));

        // Usage counts using jtokkit
        OpenAiChatResponse.Usage usage = new OpenAiChatResponse.Usage();
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

    /**
     * Streaming version returning a Flux of OpenAI-compatible streaming chunks.
     */
    public Flux<OpenAiChatStreamChunk> chatStream(OpenAiChatRequest request, boolean useRag) {
        // Detect title request from the first user message, and short-circuit
        List<Message> springMessages = this.handleMessage(request, useRag);

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
        
        Prompt granitePrompt = GraniteHelper.toGranitePrompt(springMessages);        
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
                    OpenAiChatStreamChunk chunk = new OpenAiChatStreamChunk();
                    chunk.setId(id);
                    chunk.setModel(model);
                    OpenAiChatStreamChunk.ChoiceDelta choice = new OpenAiChatStreamChunk.ChoiceDelta();
                    choice.setIndex(index.get());
                    OpenAiChatStreamChunk.Delta delta = new OpenAiChatStreamChunk.Delta();
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
                    OpenAiChatStreamChunk done = new OpenAiChatStreamChunk();
                    done.setId(id);
                    done.setModel(model);
                    OpenAiChatStreamChunk.ChoiceDelta choice = new OpenAiChatStreamChunk.ChoiceDelta();
                    choice.setIndex(index.get());
                    choice.setFinishReason("stop");
                    OpenAiChatStreamChunk.Delta delta = new OpenAiChatStreamChunk.Delta();
                    delta.setContent("");
                    choice.setDelta(delta);
                    done.setChoices(List.of(choice));
                    return done;
                }));
    }
}

package io.github.jrohila.simpleragserver.chat;

import io.github.jrohila.simpleragserver.chat.util.BoostTermDetector;
import io.github.jrohila.simpleragserver.chat.util.BoostTerm; // new
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

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
import io.github.jrohila.simpleragserver.dto.SearchResultDTO;
import io.github.jrohila.simpleragserver.service.SearchService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional; // added
// Add detector import
import io.github.jrohila.simpleragnlp.TitleRequestDetector; // assumes detector is in this package
import java.lang.reflect.InvocationTargetException;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private BoostTermDetector boostTermDetector;
    
    private final ChatModel chatModel;
    private final SearchService searchService;
    private final String ragContextPrompt;
    private final String systemAppend;
    private final TitleRequestDetector titleRequestDetector; // added
    private final Encoding tokenizer; // jtokkit tokenizer
    @Value("${processing.chat.rag.max-results:200}")
    private int ragMaxResults;
    @Value("${opensearch.rrf.window-size:50}")
    private int rrfWindowSize;
    @Value("${processing.chat.token.max-context-tokens:90000}")
    private int maxContextTokens;
    @Value("${processing.chat.token.reserve-completion:4000}")
    private int reserveCompletionTokens;
    @Value("${processing.chat.token.reserve-headroom:2000}")
    private int reserveHeadroomTokens;

    @Autowired
    public ChatService(ChatModel chatModel, SearchService searchService,
            // detector is optional to avoid failing if bean not present
            Optional<TitleRequestDetector> titleRequestDetector,
            @Value("${processing.chat.rag.context-prompt}") String ragContextPrompt,
            @Value("${processing.chat.system.append}") String systemAppend) {
        this.chatModel = chatModel;
        this.searchService = searchService;
        this.ragContextPrompt = ragContextPrompt;
        this.systemAppend = systemAppend;
        this.titleRequestDetector = titleRequestDetector != null ? titleRequestDetector.orElse(null) : null;
    // Default to CL100K_BASE (OpenAI GPT-family). For non-OpenAI models this is an approximation.
    this.tokenizer = Encodings
        .newDefaultEncodingRegistry()
        .getEncoding(EncodingType.CL100K_BASE);
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
            // Build RAG context similar to chat()
            String userPrompt = null;
            if (request.getMessages() != null) {
                for (OpenAiChatRequest.Message m : request.getMessages()) {
                    if ("user".equals(m.getRole())) {
                        userPrompt = m.getContentAsString();
                    }
                }
            }
            // Search for relevant chunks using the prompt if useRag is true
            if (useRag) {
                springMessages = this.addContext(userPrompt, springMessages);
            }

            // DEBUG: log exactly what will be sent to the LLM (streaming)
            if (log.isDebugEnabled()) {
                log.debug("[ChatService] Streaming: sending to LLM ({} messages):", springMessages.size());
                int i = 0;
                for (Message msg : springMessages) {
                    log.debug("  #{} [{}] {}", i++, msg.getClass().getSimpleName(), extractMessageText(msg));
                }
            }
        }
        return springMessages;
    }

    private List<Message> addContext(String userPrompt, List<Message> springMessages) {
        String context = "";
        if (userPrompt != null && !userPrompt.isBlank()) {
            try {
                List<BoostTerm> additionalBoostTerms = boostTermDetector.getBoostTermsByNouns(springMessages);
                
                // Use boosted hybrid search with knn; dynamically sized
                // Choose a coherent candidate set size: ensure at least RRF window size, and use ragMaxResults to cap
                int minNeeded = Math.max(25, rrfWindowSize);
                int size = Math.max(minNeeded, ragMaxResults);
                log.info("[ChatService] RAG search size={} (minNeeded={}, rrfWindowSize={}, ragMaxResults={})", size, minNeeded, rrfWindowSize, ragMaxResults);
                List<SearchResultDTO> results = searchService.search(
                        userPrompt,
                        SearchService.MatchType.MATCH,
                        true,
                        size,
                        additionalBoostTerms
                );
                if (results != null && !results.isEmpty()) {
                    // Compute token budget based on current messages (without context yet)
                    int currentTokens = 0;
                    try { currentTokens = countTokensForMessages(springMessages); } catch (Exception ignore) {}
                    String prefix = (ragContextPrompt != null ? ragContextPrompt.trim() : "");
                    int prefixTokens = countTokens(prefix);
                    int budget = Math.max(0, maxContextTokens - currentTokens - prefixTokens - reserveCompletionTokens - reserveHeadroomTokens);
                    if (budget <= 0) {
                        log.info("[ChatService] Context budget is 0 or negative (currentTokens={}, prefixTokens={}). Skipping RAG context.", currentTokens, prefixTokens);
                    } else {
                        StringBuilder sb = new StringBuilder();
                        int used = 0;
                        int added = 0;
                        for (SearchResultDTO r : results) {
                            String t = r.getText();
                            if (t == null || t.isBlank()) continue;
                            String normalized = t.trim();
                            int tokens = countTokens(normalized) + 1; // +1 for newline separator
                            if (used + tokens > budget) {
                                break;
                            }
                            sb.append(normalized).append('\n');
                            used += tokens;
                            added++;
                        }
                        context = sb.toString();
                        log.info("[ChatService] RAG packing: results={} addedChunks={} contextTokensUsed={} budget={} currentTokens={} prefixTokens={} reserve={} headroom={} ",
                                (results == null ? 0 : results.size()), added, used, budget, currentTokens, prefixTokens, reserveCompletionTokens, reserveHeadroomTokens);
                    }
                }
            } catch (Exception e) {
                log.warn("[ChatService] RAG search failed: {}", e.getMessage());
            }
        }

        // Add context as a system message if found
        if (!context.isBlank()) {
            String prefix = (ragContextPrompt != null ? ragContextPrompt.trim() : "");
            if (!prefix.isEmpty()) {
                springMessages.add(0, new SystemMessage(prefix + "\n" + context));
            } else {
                springMessages.add(0, new SystemMessage(context));
            }
        } else if (context.isBlank()) {
            log.info("[ChatService] No context found for prompt: '{}'. RAG enabled but no relevant chunks found.", userPrompt);
        }
        return springMessages;
    }

    // jtokkit helpers for Spring AI messages and plain strings
    private int countTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return tokenizer.countTokens(text);
    }

    private int countTokensForMessages(List<Message> msgs) {
        if (msgs == null) return 0;
        int total = 0;
        for (Message msg : msgs) {
            total += countTokens(extractMessageText(msg));
        }
        return total;
    }

    // Helper to extract a readable text from Spring AI Message implementations.
    private String extractMessageText(Message msg) {
        try {
            switch (msg) {
                case SystemMessage sm -> {
                    return sm.getText();
                }
                case UserMessage um -> {
                    return um.getText();
                }
                case AssistantMessage am -> {
                    return am.getText();
                }
                default -> {
                    return msg.toString();
                }
            }
        } catch (Exception e) {
            return msg.toString();
        }
    }

    public OpenAiChatResponse chat(OpenAiChatRequest request, boolean useRag) {
        // Detect title request from the first user message, and short-circuit
        List<Message> springMessages = this.handleMessage(request, useRag);

        Prompt prompt = new Prompt(springMessages);
        // Log prompt token length using jtokkit
        try {
            int promptTokens = countTokensForMessages(springMessages);
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
        if (useRag) {
            log.info("[ChatService] LLM response:");
            log.info(assistantContent);
        }

        // Log completion token length using jtokkit
        int completionTokensCount = 0;
        try {
            completionTokensCount = countTokens(assistantContent);
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
            promptTokens = countTokensForMessages(springMessages);
        } catch (Exception ignore) {}
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

        Prompt prompt = new Prompt(springMessages);
        // Log prompt token length using jtokkit
        try {
            int promptTokens = countTokensForMessages(springMessages);
            log.info("[ChatService] Tokens (prompt via jtokkit/CL100K, streaming): {} tokens across {} messages", promptTokens, springMessages.size());
        } catch (Exception e) {
            log.debug("[ChatService] Token count (prompt, streaming) failed: {}", e.getMessage());
        }
        String model = request.getModel();
        String id = "chatcmpl-" + UUID.randomUUID();
        AtomicBoolean first = new AtomicBoolean(true);
        StringBuilder cumulative = new StringBuilder();
        AtomicInteger index = new AtomicInteger(0);

        return chatModel.stream(prompt)
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
                        int completionTokens = countTokens(cumulative.toString());
                        log.info("[ChatService] Tokens (completion via jtokkit/CL100K, streaming): {} tokens", completionTokens);
                    } catch (Exception e) {
                        log.debug("[ChatService] Token count (completion, streaming) failed: {}", e.getMessage());
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

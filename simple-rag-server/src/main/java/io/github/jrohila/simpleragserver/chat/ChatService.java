package io.github.jrohila.simpleragserver.chat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatModel chatModel;
    private final SearchService searchService;

    @org.springframework.beans.factory.annotation.Autowired
    public ChatService(ChatModel chatModel, SearchService searchService) {
        this.chatModel = chatModel;
        this.searchService = searchService;
    }

    public OpenAiChatResponse chat(OpenAiChatRequest request) {
        return chat(request, true);
    }

    public OpenAiChatResponse chat(OpenAiChatRequest request, boolean useRag) {
        List<Message> springMessages = new ArrayList<>();
        String userPrompt = null;
        if (request.getMessages() != null) {
            for (OpenAiChatRequest.Message m : request.getMessages()) {
                if (m.getRole() == null) continue;
                switch (m.getRole()) {
                    case "system" -> springMessages.add(new SystemMessage(m.getContentAsString()));
                    case "user" -> {
                        springMessages.add(new UserMessage(m.getContentAsString()));
                        userPrompt = m.getContentAsString();
                    }
                    case "assistant" -> springMessages.add(new AssistantMessage(m.getContentAsString()));
                    default -> springMessages.add(new UserMessage(m.getContentAsString()));
                }
            }
        }

        // Search for relevant chunks using the prompt if useRag is true
        String context = "";
        if (useRag && userPrompt != null && !userPrompt.isBlank()) {
            try {
                // Use boosted hybrid search with knn; limit to 8 chunks for prompt budget
                List<SearchResultDTO> results = searchService.search(
                        userPrompt,
                        SearchService.MatchType.MATCH,
                        true,
                        8
                );
                if (results != null && !results.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (SearchResultDTO r : results) {
                        String t = r.getText();
                        if (t != null && !t.isBlank()) {
                            sb.append(t.trim()).append("\n");
                        }
                    }
                    context = sb.toString();
                }
            } catch (Exception e) {
                log.warn("[ChatService] RAG search failed: {}", e.getMessage());
            }
        }

        // Add context as a system message if found
        if (useRag && !context.isBlank()) {
            springMessages.add(0, new SystemMessage("Use only the following context to answer:\n" + context));
        } else if (useRag && context.isBlank()) {
            log.info("[ChatService] No context found for prompt: '{}'. RAG enabled but no relevant chunks found.", userPrompt);
        }

        Prompt prompt = new Prompt(springMessages);
        if (useRag) {
            log.info("[ChatService] Prompt sent to LLM:");
            for (Message msg : springMessages) {
                log.info("  [{}] {}", msg.getClass().getSimpleName(), msg.toString());
            }
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
                } catch (Exception e1) {
                    try {
                        var mContent = outMsg.getClass().getMethod("getContent");
                        Object v2 = mContent.invoke(outMsg);
                        assistantContent = v2 != null ? v2.toString() : "";
                    } catch (Exception e2) {
                        assistantContent = outMsg.toString();
                    }
                }
            }
        }
        if (useRag) {
            log.info("[ChatService] LLM response:");
            log.info(assistantContent);
        }

        OpenAiChatResponse out = new OpenAiChatResponse();
        out.setId("chatcmpl-" + UUID.randomUUID());
        out.setModel(request.getModel());

        OpenAiChatResponse.Choice choice = new OpenAiChatResponse.Choice();
        choice.setIndex(0);
        choice.setFinishReason("stop");
        choice.setMessage(new OpenAiChatRequest.Message("assistant", assistantContent));
        out.setChoices(List.of(choice));

        // Basic usage estimation (placeholders; real token counting would require model-specific logic)
        OpenAiChatResponse.Usage usage = new OpenAiChatResponse.Usage();
        int promptTokens = estimateTokens(request.getMessages());
        List<OpenAiChatRequest.Message> single = new ArrayList<>();
        single.add(new OpenAiChatRequest.Message("assistant", assistantContent));
        int completionTokens = estimateTokens(single);
        usage.setPromptTokens(promptTokens);
        usage.setCompletionTokens(completionTokens);
        usage.setTotalTokens(promptTokens + completionTokens);
        out.setUsage(usage);
        return out;
    }

    private int estimateTokens(List<OpenAiChatRequest.Message> messages) {
        if (messages == null) return 0;
        int total = 0;
        for (OpenAiChatRequest.Message m : messages) {
            String content = m.getContentAsString();
            if (content != null) {
                total += Math.max(1, content.length() / 4); // rough heuristic
            }
        }
        return total;
    }

    /**
     * Streaming version returning a Flux of OpenAI-compatible streaming chunks.
     */
    public Flux<OpenAiChatStreamChunk> chatStream(OpenAiChatRequest request) {
        List<Message> springMessages = new ArrayList<>();
        if (request.getMessages() != null) {
            for (OpenAiChatRequest.Message m : request.getMessages()) {
                if (m.getRole() == null) continue;
                switch (m.getRole()) {
                    case "system" -> springMessages.add(new SystemMessage(m.getContentAsString()));
                    case "user" -> springMessages.add(new UserMessage(m.getContentAsString()));
                    case "assistant" -> springMessages.add(new AssistantMessage(m.getContentAsString()));
                    default -> springMessages.add(new UserMessage(m.getContentAsString()));
                }
            }
        }
        Prompt prompt = new Prompt(springMessages);
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
            .concatWith(Mono.fromSupplier(() -> { // final stop chunk
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

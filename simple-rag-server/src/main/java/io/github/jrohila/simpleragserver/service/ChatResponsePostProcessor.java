package io.github.jrohila.simpleragserver.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.github.jrohila.simpleragserver.domain.ExtractedFactDTO;
import io.github.jrohila.simpleragserver.domain.ExtractedFactsDTO;
import io.github.jrohila.simpleragserver.dto.MessageDTO;
import io.github.jrohila.simpleragserver.util.LlmOutputCleaner;
import io.github.jrohila.simpleragserver.client.LlmClientFactory;
import io.github.jrohila.simpleragserver.client.LlmClient;
import io.github.jrohila.simpleragserver.client.LlmRequestOptions;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.Response;

@Service
public class ChatResponsePostProcessor implements ChatStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChatResponsePostProcessor.class);

    public Map<String, Pair<List<MessageDTO>, List<Integer>>> contexts = new HashMap<>();

    @Autowired(required = false)
    private LlmClientFactory llmClientFactory;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private UserFactsService userFactsService;

    @Value("${processing.post.chat.fact.extractor.append:}")
    private String factExtractorTemplate;

    public void addContext(String streamId, List<MessageDTO> messages, List<Integer> tokens) {
        contexts.put(streamId, Pair.of(messages, tokens));
    }

    @Override
    public void onDelta(String streamId, String delta) {
        if (delta == null || delta.isEmpty()) {
        }
    }

    @Override
    public void onComplete(String streamId, String fullResponse) {
        if (fullResponse == null) {
            log.debug("[StreamCapture] {} complete (empty response)", streamId);
        } else {
            Pair<List<MessageDTO>, List<Integer>> context = contexts.get(streamId);
            if (context != null) {
                List<MessageDTO> original = context.getLeft();
                List<MessageDTO> reversed = new ArrayList<>(original);
                Collections.reverse(reversed);
                for (MessageDTO message : reversed) {
                    if (MessageDTO.Role.USER.equals(message.getRole())) {
                        String userText = message.getContentAsString();
                        log.info("[PostProcessor] Latest USER message: {}", userText);

                        // Build system prompt from template if available
                        if (factExtractorTemplate == null || factExtractorTemplate.isBlank()) {
                            log.debug("[PostProcessor] fact extractor template is blank, property processing.post.chat.fact.extractor.append");
                            break;
                        }
                        String systemPrompt = factExtractorTemplate.replace("{{user_message}}", userText == null ? "" : userText);

                        // Get default LLM client
                        LlmClient client = null;
                        try {
                            client = llmClientFactory.getDefaultClient();
                        } catch (Exception e) {
                            log.debug("[PostProcessor] No LLM client available: {}", e.getMessage());
                        }
                        if (client == null) {
                            log.debug("[PostProcessor] LlmClient not available, skipping fact extraction");
                            break;
                        }

                        try {
                            List<ChatMessage> msgs = List.of(new SystemMessage(systemPrompt));
                            LlmRequestOptions opts = LlmRequestOptions.defaults();
                            Response<String> resp = client.chat(msgs, opts);
                            String assistantOut = resp != null && resp.content() != null ? resp.content() : "";
                            log.info("[PostProcessor] Fact extractor response: {}", assistantOut);

                            assistantOut = LlmOutputCleaner.getJson(assistantOut);
                            
                            // Parse assistantOut JSON into ExtractedFactDTO(s) and update facts
                            List<ExtractedFactDTO> parsedFacts = parseFactsFromJson(assistantOut);
                            if (parsedFacts != null && !parsedFacts.isEmpty()) {
                                List<Integer> tokens = context.getRight();
                                if (userFactsService != null) {
                                    userFactsService.updateFacts(parsedFacts, tokens);
                                    log.info("[PostProcessor] Parsed {} fact(s) and updated UserFactsService", parsedFacts.size());
                                } else {
                                    log.debug("[PostProcessor] UserFactsService not available; parsed facts not stored");
                                }
                            } else {
                                log.debug("[PostProcessor] No valid facts parsed from assistant output");
                            }
                        } catch (Exception e) {
                            log.warn("[PostProcessor] Fact extractor call failed: {}", e.getMessage());
                        }
                        break;
                    }
                }
            }

        }
    }

    private List<ExtractedFactDTO> parseFactsFromJson(String json) {
        if (json == null || json.isBlank() || objectMapper == null) return Collections.emptyList();
        try {
            ObjectMapper mapper = objectMapper.copy();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            String trimmed = json.trim();
            if (trimmed.startsWith("{")) {
                // Try wrapper first
                try {
                    ExtractedFactsDTO wrapper = mapper.readValue(trimmed, ExtractedFactsDTO.class);
                    if (wrapper != null && wrapper.getFacts() != null && !wrapper.getFacts().isEmpty()) {
                        return wrapper.getFacts();
                    }
                } catch (Exception ignore) {
                    // fallback to single object below
                }
                // Fallback to single object
                ExtractedFactDTO one = mapper.readValue(trimmed, ExtractedFactDTO.class);
                return List.of(one);
            } else if (trimmed.startsWith("[")) {
                List<ExtractedFactDTO> list = mapper.readValue(trimmed, new TypeReference<List<ExtractedFactDTO>>(){});
                return list;
            } else {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.debug("[PostProcessor] Failed to parse facts JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}

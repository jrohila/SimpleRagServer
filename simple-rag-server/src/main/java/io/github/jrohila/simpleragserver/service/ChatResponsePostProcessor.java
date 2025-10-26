package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.chat.ChatStreamConsumer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.github.jrohila.simpleragserver.dto.ExtractedFactDTO;
import io.github.jrohila.simpleragserver.dto.ExtractedFactsDTO;

@Service
public class ChatResponsePostProcessor implements ChatStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChatResponsePostProcessor.class);

    public Map<String, Pair<List<Message>, List<Integer>>> contexts = new HashMap<>();

    @Autowired(required = false)
    private ChatModel chatModel;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private UserFactsService userFactsService;

    @Value("${processing.post.chat.fact.extractor.append:}")
    private String factExtractorTemplate;

    public void addContext(String streamId, List<Message> messages, List<Integer> tokens) {
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
            Pair<List<Message>, List<Integer>> context = contexts.get(streamId);
            if (context != null) {
                List<Message> original = context.getLeft();
                List<Message> reversed = new ArrayList<>(original);
                Collections.reverse(reversed);
                for (Message message : reversed) {
                    if (MessageType.USER.equals(message.getMessageType())) {
                        String userText = message.getText();
                        log.info("[PostProcessor] Latest USER message: {}", userText);

                        // Build system prompt from template if available
                        if (chatModel == null) {
                            log.debug("[PostProcessor] ChatModel not available, skipping fact extraction");
                            break;
                        }
                        if (factExtractorTemplate == null || factExtractorTemplate.isBlank()) {
                            log.debug("[PostProcessor] fact extractor template is blank, property processing.post.chat.fact.extractor.append");
                            break;
                        }
                        String systemPrompt = factExtractorTemplate.replace("{{user_message}}", userText == null ? "" : userText);

                        try {
                            ChatResponse resp = chatModel.call(new Prompt(List.of(new SystemMessage(systemPrompt))));
                            String assistantOut = "";
                            if (resp != null && resp.getResults() != null && !resp.getResults().isEmpty()) {
                                var outMsg = resp.getResults().get(0).getOutput();
                                if (outMsg instanceof AssistantMessage am) {
                                    assistantOut = am.getText();
                                } else if (outMsg != null) {
                                    assistantOut = outMsg.toString();
                                }
                            }
                            log.info("[PostProcessor] Fact extractor response: {}", assistantOut);

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

package io.github.jrohila.simpleragserver.dto;

import io.github.jrohila.simpleragserver.dto.MessageDTO;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Minimal OpenAI-compatible Chat Completions response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiChatResponseDTO {
    private String id;
    private String object = "chat.completion";
    private long created = Instant.now().getEpochSecond();
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private int index;
        private MessageDTO message;
        private String finishReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }
}

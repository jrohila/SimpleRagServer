package io.github.jrohila.simpleragserver.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * OpenAI-compatible streaming chunk (object: 'chat.completion.chunk').
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiChatStreamChunkDTO {
    private String id;
    @SuppressWarnings("unused")
    private String object = "chat.completion.chunk";
    private long created = Instant.now().getEpochSecond();
    private String model;
    private List<ChoiceDelta> choices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChoiceDelta {
        private int index;
        private Delta delta;
        private String finishReason; // null until final
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Delta {
        private String role; // only on first chunk
        private String content; // incremental content
    }
}

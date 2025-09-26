package io.github.jrohila.simpleragserver.chat;

import java.time.Instant;
import java.util.List;

/**
 * OpenAI-compatible streaming chunk (object: 'chat.completion.chunk').
 */
public class OpenAiChatStreamChunk {
    private String id;
    @SuppressWarnings("unused")
    private String object = "chat.completion.chunk";
    private long created = Instant.now().getEpochSecond();
    private String model;
    private List<ChoiceDelta> choices;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getCreated() { return created; }
    public void setCreated(long created) { this.created = created; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<ChoiceDelta> getChoices() { return choices; }
    public void setChoices(List<ChoiceDelta> choices) { this.choices = choices; }

    public static class ChoiceDelta {
        private int index;
        private Delta delta;
        private String finishReason; // null until final
        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public Delta getDelta() { return delta; }
        public void setDelta(Delta delta) { this.delta = delta; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }

    public static class Delta {
        private String role; // only on first chunk
        private String content; // incremental content
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}

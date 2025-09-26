package io.github.jrohila.simpleragserver.chat;

import java.util.List;

/**
 * Minimal OpenAI-compatible Chat Completions request DTO.
 */
public class OpenAiChatRequest {
    private String model; // optional override; default from properties
    private List<Message> messages; // required
    private Double temperature;
    private Integer maxTokens; // maps to max_output_tokens or similar
    private boolean stream = false;

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }

    public static class Message {
        private String role; // system|user|assistant|tool
        private String content;
        public Message() {}
        public Message(String role, String content) { this.role = role; this.content = content; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}

package io.github.jrohila.simpleragserver.chat;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;
import java.util.HashMap;

/**
 * Minimal OpenAI-compatible Chat Completions request DTO.
 */
public class OpenAiChatRequest {
    private String model; // optional override; default from properties
    private List<Message> messages; // required
    private Double temperature;
    @JsonProperty("max_tokens")
    private Integer maxTokens; // maps to max_output_tokens or similar
    private boolean stream = false;
    
    // Extended parameters for Ollama and other LLM backends
    @JsonProperty("top_p")
    private Double topP;
    @JsonProperty("top_k")
    private Integer topK;
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;
    @JsonProperty("min_tokens")
    private Integer minTokens;
    @JsonProperty("do_sample")
    private Boolean doSample;

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
    
    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public Double getFrequencyPenalty() { return frequencyPenalty; }
    public void setFrequencyPenalty(Double frequencyPenalty) { this.frequencyPenalty = frequencyPenalty; }
    public Integer getMinTokens() { return minTokens; }
    public void setMinTokens(Integer minTokens) { this.minTokens = minTokens; }
    public Boolean getDoSample() { return doSample; }
    public void setDoSample(Boolean doSample) { this.doSample = doSample; }

    public static class Message {
        private String role; // system|user|assistant|tool
        private JsonNode content; // Can be string or array
        private Map<String, Object> additionalProperties = new HashMap<>();
        
        public Message() {}
        public Message(String role, String content) { 
            this.role = role; 
            this.content = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(content);
        }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public JsonNode getContent() { return content; }
        public void setContent(JsonNode content) { this.content = content; }
        
        // Convenience method to get content as string
        public String getContentAsString() {
            if (content == null) return null;
            if (content.isTextual()) {
                return content.asText();
            } else if (content.isArray() && content.size() > 0) {
                // For array content, extract text from first text element
                for (JsonNode item : content) {
                    if (item.has("type") && "text".equals(item.get("type").asText())) {
                        return item.get("text").asText();
                    }
                }
                return content.toString();
            }
            return content.toString();
        }
        
        @JsonAnySetter
        public void setAdditionalProperty(String name, Object value) {
            this.additionalProperties.put(name, value);
        }
    }
}

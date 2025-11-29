package io.github.jrohila.simpleragserver.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.HashMap;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    private Role role; // system|user|assistant|tool
    private JsonNode content; // Can be string or array
    private Map<String, Object> additionalProperties = new HashMap<>();

    // Keep a convenience constructor that accepts role as string for existing call sites
    public MessageDTO(String role, String content) {
        this.role = Role.fromValue(role);
        this.content = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(content);
    }

    // Convenience method to get content as string
    public String getContentAsString() {
        if (content == null) {
            return null;
        }
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

    @Getter
    @RequiredArgsConstructor
    public enum Role {
        SYSTEM("system"),
        USER("user"),
        ASSISTANT("assistant"),
        TOOL("tool");

        private final String value;

        @JsonValue
        public String toValue() { return value; }

        @JsonCreator
        public static Role fromValue(String v) {
            if (v == null) return null;
            for (Role r : values()) {
                if (r.value.equalsIgnoreCase(v)) return r;
            }
            throw new IllegalArgumentException("Unknown role: " + v);
        }
    }
}

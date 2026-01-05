package io.github.jrohila.simpleragserver.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;
import java.util.HashMap;
import io.github.jrohila.simpleragserver.dto.MessageDTO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Minimal OpenAI-compatible Chat Completions request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiChatRequestDTO {
    private String model; // optional override; default from properties
    private List<MessageDTO> messages; // required
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

}
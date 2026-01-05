package io.github.jrohila.simpleragserver.client;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request options for LLM chat completions.
 */
@Data
@Builder
public class LlmRequestOptions {
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Integer topK;
    private Double frequencyPenalty;
    private Double presencePenalty;
    private List<String> stopSequences;
    
    public static LlmRequestOptions defaults() {
        return LlmRequestOptions.builder().build();
    }
}

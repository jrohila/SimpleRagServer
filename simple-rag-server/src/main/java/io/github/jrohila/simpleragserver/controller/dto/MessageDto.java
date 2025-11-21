package io.github.jrohila.simpleragserver.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message DTO for WebGPU LLM request/response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    /**
     * Message role: "system", "user", or "assistant"
     */
    private String role;
    
    /**
     * Message content
     */
    private String content;
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.util;

import io.github.jrohila.simpleragserver.dto.MessageDTO;
import java.util.List;
/**
 * Utility class for formatting chat prompts for IBM Granite LLMs.
 * <p>
 * IBM Granite models require special prompt tokens to indicate message roles
 * and boundaries. This helper converts a list of Spring AI {@link Message}
 * objects into a single prompt string using Granite's required format:
 * <pre>
 *   &lt;|start_of_role|&gt;system&lt;|end_of_role|&gt;System message here&lt;|end_of_text|&gt;
 *   &lt;|start_of_role|&gt;user&lt;|end_of_role|&gt;User message here&lt;|end_of_text|&gt;
 *   &lt;|start_of_role|&gt;assistant&lt;|end_of_role|&gt;Assistant message here&lt;|end_of_text|&gt;
 * </pre>
 * <p>
 * The {@link #toGranitePrompt(List)} method wraps the result as a single
 * {@link UserMessage} for use with Spring AI's {@link Prompt} abstraction,
 * ensuring the LLM receives the prompt in the correct format for optimal
 * response quality.
 *
 * Example usage:
 * <pre>
 *   Prompt granitePrompt = GraniteHelper.toGranitePrompt(messages);
 * </pre>
 *
 * @author Jukka
 */
public class GraniteHelper {

    public static String toGranitePrompt(List<MessageDTO> messages) {
        StringBuilder sb = new StringBuilder();
        for (MessageDTO msg : messages) {
            String role = msg.getRole().getValue();
            sb.append("<|start_of_role|>").append(role)
                    .append("<|end_of_role|>")
                    .append(msg.getContentAsString())
                    .append("<|end_of_text|>\n");
        }
        return sb.toString();
    }
    
}

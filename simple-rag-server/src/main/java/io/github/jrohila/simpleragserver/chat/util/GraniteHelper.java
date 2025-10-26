/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.chat.util;

import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Utility class for formatting chat prompts for IBM Granite LLMs.
 * <p>
 * IBM Granite models require special prompt tokens to indicate message roles and boundaries.
 * This helper converts a list of Spring AI {@link Message} objects into a single prompt string
 * using Granite's required format:
 * <pre>
 *   &lt;|start_of_role|&gt;system&lt;|end_of_role|&gt;System message here&lt;|end_of_text|&gt;
 *   &lt;|start_of_role|&gt;user&lt;|end_of_role|&gt;User message here&lt;|end_of_text|&gt;
 *   &lt;|start_of_role|&gt;assistant&lt;|end_of_role|&gt;Assistant message here&lt;|end_of_text|&gt;
 * </pre>
 * <p>
 * The {@link #toGranitePrompt(List)} method wraps the result as a single {@link UserMessage}
 * for use with Spring AI's {@link Prompt} abstraction, ensuring the LLM receives the prompt
 * in the correct format for optimal response quality.
 *
 * Example usage:
 * <pre>
 *   Prompt granitePrompt = GraniteHelper.toGranitePrompt(messages);
 * </pre>
 *
 * @author Jukka
 */
public class GraniteHelper {

    public static Prompt toGranitePrompt(List<Message> messages) {
        String granitePrompt = toString(messages);
        return new Prompt(List.of(new UserMessage(granitePrompt)));
    }

    public static String toString(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            String role = "user";
            if (msg instanceof SystemMessage) {
                role = "system";
            } else if (msg instanceof AssistantMessage) {
                role = "assistant";
            }
            sb.append("<|start_of_role|>").append(role)
                    .append("<|end_of_role|>")
                    .append(msg.getText())
                    .append("<|end_of_text|>\n");
        }
        return sb.toString();
    }

}

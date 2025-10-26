/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.chat.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/**
 *
 * @author Jukka
 */
@Component
public class ChatHelper {

    // Default to CL100K_BASE (OpenAI GPT-family). For non-OpenAI models this is an approximation.
    private final Encoding tokenizer = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    // jtokkit helpers for Spring AI messages and plain strings
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return tokenizer.countTokens(text);
    }

    public int countTokensForMessages(List<Message> msgs) {
        if (msgs == null) {
            return 0;
        }
        int total = 0;
        for (Message msg : msgs) {
            total += countTokens(extractMessageText(msg));
        }
        return total;
    }

    // Helper to extract a readable text from Spring AI Message implementations.
    public String extractMessageText(Message msg) {
        try {
            switch (msg) {
                case SystemMessage sm -> {
                    return sm.getText();
                }
                case UserMessage um -> {
                    return um.getText();
                }
                case AssistantMessage am -> {
                    return am.getText();
                }
                default -> {
                    return msg.toString();
                }
            }
        } catch (Exception e) {
            return msg.toString();
        }
    }

}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import io.github.jrohila.simpleragserver.dto.MessageDTO;
import java.util.List;
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

    public int countTokensForMessages(List<MessageDTO> msgs) {
        if (msgs == null) {
            return 0;
        }
        int total = 0;
        for (MessageDTO msg : msgs) {
            total += countTokens(msg.getContentAsString());
        }
        return total;
    }

}

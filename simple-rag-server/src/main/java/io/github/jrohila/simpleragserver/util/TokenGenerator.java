/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.util;

import io.github.jrohila.simpleragserver.dto.MessageDTO;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.zip.CRC32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jukka
 */
public class TokenGenerator {

    private static final Logger log = LoggerFactory.getLogger(TokenGenerator.class);
    private static final char DELIM = '\u001F'; // Unit Separator

    /**
     * Generate up to 5 tokens using sliding windows assuming newest message is
     * at index 0. Windows are: - 0+1+2+3+4 (or fewer if not enough messages) -
     * 1+2+3+4+5 - 2+3+4+5(+6 if available) ... up to 5 tokens total. Within
     * each window, texts are concatenated from oldest -> newest for stability.
     * Contract: - Input: list of Message (can be null/empty) - Output: list of
     * integer tokens (0..5 items) - Stable hashing: CRC32 (int) over UTF-8
     * concatenation with separator
     */
    public static List<Integer> createTokens(List<MessageDTO> messages) {
        List<MessageDTO> userMessages = new ArrayList<>();
        for (MessageDTO message : messages) {
            if (MessageDTO.Role.USER.equals(message.getRole())) {
                userMessages.add(message);
            }
        }
        Collections.reverse(userMessages);

        log.info("[TokenGenerator] user messages={}", userMessages.size());

        List<Integer> tokens = new ArrayList<>();
        if (userMessages.isEmpty()) {
            return tokens;
        }
        final int size = userMessages.size();
        final int maxTokens = Math.min(3, size); // produce at most 5 tokens

        // Sliding windows starting at 0: [0..W-1], [1..1+W-1], ... up to 5 tokens total
        // W for a given start is min(5, size - start). Concatenate oldest->newest within each window.
        for (int start = 0; start < maxTokens; start++) {
            StringBuilder sb = new StringBuilder();

            int windowLen = 2 + start;
            if (size < windowLen) {
                windowLen = size;
            }
            for (int i = start; i < windowLen; i++) {
                MessageDTO m = userMessages.get(i);
                if (m == null) {
                    continue;
                }
                String text = m.getContentAsString();
                if (text == null) {
                    text = "";
                }
                if (sb.length() > 0) {
                    sb.append(DELIM); // delimiter
                }
                sb.append(text);
            }
            int token = hashToInt(sb.toString());
            tokens.add(token);
        }
        return tokens;
    }

    private static int hashToInt(String input) {
        CRC32 crc = new CRC32();
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        crc.update(bytes, 0, bytes.length);
        long value = crc.getValue();
        return (int) (value & 0xFFFFFFFFL);
    }

}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.chat.util;

/**
 *
 * @author Jukka
 */
// Maven deps: org.apache.commons:commons-text for LevenshteinDistance
import org.apache.commons.text.similarity.LevenshteinDistance;
import java.util.regex.Pattern;

public class TitleRequestDetector {

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "(give( this)? (conversation|chat) (a )?name|based on the chat history, give (this )?conversation a name|generate (a )?title)",
            Pattern.CASE_INSENSITIVE
    );

    private final LevenshteinDistance ld = LevenshteinDistance.getDefaultInstance();

    public boolean isTitleRequest(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (TITLE_PATTERN.matcher(text).find()) {
            return true;
        }

        // fuzzy fallback for short texts
        String canonical = "[UserMessage] Based on the chat history, give this conversation a name.";
        int distance = ld.apply(text.toLowerCase(), canonical.toLowerCase());
        int threshold = 12; // tune: lower for short strings, higher for long ones
        return distance <= threshold;
    }
}

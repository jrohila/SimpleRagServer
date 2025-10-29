package io.github.jrohila.simpleragserver.util;

public class LlmOutputCleaner {
    /**
     * Extracts the JSON content from LLM output, handling code blocks, decorations, and extra text.
     * @param output The raw LLM output string
     * @return The cleaned JSON string, or the original string if no JSON found
     */
    public static String getJson(String output) {
        if (output == null) return null;
        String trimmed = output.trim();

        // Case 1: ```json ... ```
        int jsonStart = trimmed.indexOf("```json");
        if (jsonStart != -1) {
            int afterJson = jsonStart + 7;
            int jsonEnd = trimmed.indexOf("```", afterJson);
            if (jsonEnd != -1) {
                return trimmed.substring(afterJson, jsonEnd).trim();
            }
        }

        // Case 2: ``` ... ``` (no language)
        int codeStart = trimmed.indexOf("```", 0);
        if (codeStart != -1) {
            int afterCode = codeStart + 3;
            int codeEnd = trimmed.indexOf("```", afterCode);
            if (codeEnd != -1) {
                return trimmed.substring(afterCode, codeEnd).trim();
            }
        }

        // Case 3: Fallback to first {...} block
        int braceStart = trimmed.indexOf('{');
        int braceEnd = trimmed.lastIndexOf('}');
        if (braceStart != -1 && braceEnd > braceStart) {
            return trimmed.substring(braceStart, braceEnd + 1).trim();
        }

        // No JSON found, return as is
        return trimmed;
    }
}

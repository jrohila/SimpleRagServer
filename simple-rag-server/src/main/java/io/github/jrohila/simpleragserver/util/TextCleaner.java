package io.github.jrohila.simpleragserver.util;

import java.util.regex.Pattern;

public final class TextCleaner {
    private static final Pattern URL = Pattern.compile("(?i)\\b((?:https?|ftp)://|www\\.)[\\w.-]+(?:/[\\w\\p{Punct}]*)?", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern FILE_PATH = Pattern.compile("(?i)(?:[a-zA-Z]:\\\\|/)[^\n\r\t ]+");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[[^\\]]+\\]\\([^\\)]+\\)");
    private static final Pattern MULTI_WS = Pattern.compile("[\\s\\u00A0]+");

    private TextCleaner() {}

    public static String clean(String input) {
        if (input == null || input.isBlank()) return input;
        String s = input;
        // remove markdown links completely
        s = MARKDOWN_LINK.matcher(s).replaceAll("");
        // remove urls, emails, and file paths
        s = URL.matcher(s).replaceAll("");
        s = EMAIL.matcher(s).replaceAll("");
        s = FILE_PATH.matcher(s).replaceAll("");
        // collapse whitespace
        s = MULTI_WS.matcher(s).replaceAll(" ").trim();
        return s;
    }
}

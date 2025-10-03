package io.github.jrohila.simpleragserver.pipeline;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import io.github.jrohila.simpleragserver.service.NlpService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;

@Service
public class XhtmlToChunk {

    private final NlpService nlpService;

    public XhtmlToChunk(NlpService nlpService) {
        this.nlpService = nlpService;
    }
    public List<ChunkEntity> parseChunks(String xhtml) {
        List<ChunkEntity> chunks = new ArrayList<>();
        Document doc = Jsoup.parse(xhtml, "", org.jsoup.parser.Parser.xmlParser());
        Element body = doc.body();
        if (body == null) return chunks;

        // Map for headline levels and their text (e.g., 1->h1, 2->h2, ...)
        Map<Integer, String> headlineMap = new HashMap<>();
        traverse(body, headlineMap, chunks);

        // Post-process: detect and set language for each chunk
        for (ChunkEntity chunk : chunks) {
            String lang = nlpService.detectLanguage(chunk.getText());
            chunk.setLanguage(lang);
        }
        return chunks;
    }

    private void traverse(Element el, Map<Integer, String> headlineMap, List<ChunkEntity> chunks) {
        for (Element child : el.children()) {
            String tag = child.tagName().toLowerCase();
            if (tag.matches("h[1-6]")) {
                int level = Integer.parseInt(tag.substring(1));
                String text = child.text();
                headlineMap.put(level, text);
                // Remove all deeper levels
                for (int l = level + 1; l <= 6; l++) headlineMap.remove(l);
                // Do NOT create a chunk for the heading itself
            } else if (tag.equals("p")) {
                ChunkEntity chunk = new ChunkEntity();
                chunk.setText(child.text());
                chunk.setSectionTitle(buildSectionTitle(headlineMap));
                chunk.setType("paragraph");
                chunk.setPageNumber(parsePage(child));
                // Calculate stable hash based on sectionTitle + text
                chunk.setHash(hashOf(chunk.getSectionTitle(), chunk.getText()));
                chunks.add(chunk);
            } else if (tag.equals("li")) {
                // Intentionally skip handling <li> here; list items are only chunked
                // when iterating direct children of a parent <ul> or <ol> above.
                // This prevents creating chunks for stray or out-of-context <li> elements.
            } else if (tag.equals("ul") || tag.equals("ol")) {
                // Create a single chunk for the entire list, combining all nested text
                ChunkEntity chunk = new ChunkEntity();
                chunk.setText(child.text());
                chunk.setSectionTitle(buildSectionTitle(headlineMap));
                chunk.setType("list");
                chunk.setPageNumber(parsePage(child));
                // Calculate stable hash based on sectionTitle + combined list text
                chunk.setHash(hashOf(chunk.getSectionTitle(), chunk.getText()));
                chunks.add(chunk);
                // Do not recurse into this list to avoid duplicating content from its items
                continue;
            }
            // Recurse for nested elements
            traverse(child, headlineMap, chunks);
        }
    }

    private String buildSectionTitle(Map<Integer, String> headlineMap) {
        List<String> titles = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            if (headlineMap.containsKey(i)) {
                titles.add(headlineMap.get(i));
            }
        }
        return String.join(" / ", titles);
    }

    private int parsePage(Element el) {
        String page = el.attr("page");
        try {
            return page.isEmpty() ? -1 : Integer.parseInt(page);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String hashOf(String sectionTitle, String text) {
        String s = (sectionTitle == null ? "" : sectionTitle) + "\n" + (text == null ? "" : text);
        return DigestUtils.sha256Hex(s);
    }
}

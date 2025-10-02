package io.github.jrohila.simpleragserver.pipeline;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
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
    public List<ChunkEntity> parseChunks(String xhtml) {
        List<ChunkEntity> chunks = new ArrayList<>();
        Document doc = Jsoup.parse(xhtml, "", org.jsoup.parser.Parser.xmlParser());
        Element body = doc.body();
        if (body == null) return chunks;

        // Map for headline levels and their text (e.g., 1->h1, 2->h2, ...)
        Map<Integer, String> headlineMap = new HashMap<>();
        traverse(body, headlineMap, chunks);
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
            } else if (tag.equals("ul") || tag.equals("ol")) {
                for (Element li : child.children()) {
                    if (li.tagName().equalsIgnoreCase("li")) {
                        ChunkEntity chunk = new ChunkEntity();
                        chunk.setText(li.text());
                        chunk.setSectionTitle(buildSectionTitle(headlineMap));
                        chunk.setType("li");
                        chunk.setPageNumber(parsePage(li));
                        // Calculate stable hash based on sectionTitle + text
                        chunk.setHash(hashOf(chunk.getSectionTitle(), chunk.getText()));
                        chunks.add(chunk);
                    }
                }
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

package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.model.ChunkDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class XhtmlToChunkService {
    public List<ChunkDto> parseChunks(String xhtml) {
        List<ChunkDto> chunks = new ArrayList<>();
        Document doc = Jsoup.parse(xhtml, "", org.jsoup.parser.Parser.xmlParser());
        Element body = doc.body();
        if (body == null) return chunks;

        // Map for headline levels and their text (e.g., 1->h1, 2->h2, ...)
        Map<Integer, String> headlineMap = new HashMap<>();
        traverse(body, headlineMap, chunks);
        return chunks;
    }

    private void traverse(Element el, Map<Integer, String> headlineMap, List<ChunkDto> chunks) {
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
                ChunkDto chunk = new ChunkDto();
                chunk.setText(child.text());
                chunk.setSectionTitle(buildSectionTitle(headlineMap));
                chunk.setType("paragraph");
                chunk.setPageNumber(parsePage(child));
                chunk.setCreated(Instant.now());
                chunk.setModified(Instant.now());
                chunks.add(chunk);
            } else if (tag.equals("ul") || tag.equals("ol")) {
                for (Element li : child.children()) {
                    if (li.tagName().equalsIgnoreCase("li")) {
                        ChunkDto chunk = new ChunkDto();
                        chunk.setText(li.text());
                        chunk.setSectionTitle(buildSectionTitle(headlineMap));
                        chunk.setType("li");
                        chunk.setPageNumber(parsePage(li));
                        chunk.setCreated(Instant.now());
                        chunk.setModified(Instant.now());
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
}

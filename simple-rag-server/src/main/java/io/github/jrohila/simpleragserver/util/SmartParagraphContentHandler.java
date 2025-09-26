/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.util;

import io.github.jrohila.simpleragserver.dto.Chunk;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.tika.langdetect.tika.LanguageIdentifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class SmartParagraphContentHandler extends DefaultHandler {
    
    private final List<Chunk> chunks = new ArrayList<>();
    private StringBuilder currentText = null;
    private String currentType = null;
    private String currentSectionTitle = null;
    private int currentPageNumber = -1;
    private int chunkStartPage = -1;
    private String pendingLanguage = null;
    
    // Sentence-aware paragraph buffer
    private final StringBuilder pendingParagraphBuffer = new StringBuilder();
    // Pseudo-list buffer
    private final List<String> pendingListItems = new ArrayList<>();
    
    // Section/heading stack
    private final Deque<HeadingInfo> headingStack = new ArrayDeque<>();
    private static class HeadingInfo { int level; String title;
        HeadingInfo(int level, String title) { this.level = level; this.title = title; } }
    
    // Real list stack
    private final Deque<ListContext> listStack = new ArrayDeque<>();
    private static class ListContext { boolean ordered; int itemCount;
        ListContext(boolean ordered) { this.ordered = ordered; this.itemCount = 0; } }
    
    private boolean insideCaption = false;
    private StringBuilder captionText = null;
    private int captionStartPage = -1;
    
    @Override
    public void startDocument() {
        chunks.clear();
        currentText = null;
        currentType = null;
        currentSectionTitle = null;
        currentPageNumber = -1;
        chunkStartPage = -1;
        pendingLanguage = null;
        headingStack.clear();
        listStack.clear();
        insideCaption = false;
        captionText = null;
        captionStartPage = -1;
        pendingParagraphBuffer.setLength(0);
        pendingListItems.clear();
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) {
        String name = localName.isEmpty() ? qName : localName;
        
        if (name.equalsIgnoreCase("div")) {
            String classAttr = attrs.getValue("class");
            if ("page".equalsIgnoreCase(classAttr)) {
                currentPageNumber = (currentPageNumber < 1) ? 1 : currentPageNumber + 1;
            }
        }
        
        if (name.matches("h[1-6]")) {
            flushCurrentChunk(); 
            int level = Character.isDigit(name.charAt(1)) ? name.charAt(1) - '0' : 1;
            while (!headingStack.isEmpty() && headingStack.peek().level >= level) {
                headingStack.pop();
            }
            currentSectionTitle = headingStack.isEmpty() ? null : headingStack.peek().title;
            currentText = new StringBuilder();
            currentType = "heading";
            chunkStartPage = currentPageNumber;
            pendingLanguage = null;
            return;
        }
        
        if (name.equalsIgnoreCase("p")) {
            if (currentType == null || (!currentType.equals("list") && !currentType.equals("table"))) {
                flushCurrentChunk();  
                currentType = "paragraph";
                currentText = new StringBuilder();
                currentSectionTitle = (headingStack.peek() != null) ? headingStack.peek().title : null;
                chunkStartPage = currentPageNumber;
                pendingLanguage = getLanguageAttribute(attrs);
            } else {
                if (currentText != null && currentText.length() > 0) {
                    currentText.append("\n");
                    if ("list".equals(currentType)) {
                        currentText.append(getIndentation(listStack.size())); 
                    }
                }
            }
            return;
        }
        
        if (name.equalsIgnoreCase("ul") || name.equalsIgnoreCase("ol")) {
            boolean ordered = name.equalsIgnoreCase("ol");
            if (currentType == null || (!currentType.equals("list") && !currentType.equals("table"))) {
                flushCurrentChunk();
                currentType = "list";
                currentText = new StringBuilder();
                currentSectionTitle = (headingStack.peek() != null) ? headingStack.peek().title : null;
                chunkStartPage = currentPageNumber;
            } 
            listStack.push(new ListContext(ordered));
            return;
        }
        
        if (name.equalsIgnoreCase("li")) {
            if (!listStack.isEmpty()) {
                ListContext ctx = listStack.peek();
                ctx.itemCount++;
                int depth = listStack.size();
                String indent = getIndentation(depth - 1);  
                String bullet = ctx.ordered ? ctx.itemCount + ". " : "- ";
                if (currentText == null) currentText = new StringBuilder();
                currentText.append(indent).append(bullet);
            }
            return;
        }
        
        if (name.equalsIgnoreCase("table")) {
            flushCurrentChunk();
            currentType = "table";
            currentText = new StringBuilder();
            currentSectionTitle = (headingStack.peek() != null) ? headingStack.peek().title : null;
            chunkStartPage = currentPageNumber;
            return;
        }
        
        if (name.equalsIgnoreCase("caption") || name.equalsIgnoreCase("figcaption")) {
            insideCaption = true;
            captionText = new StringBuilder();
            captionStartPage = currentPageNumber;
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) {
        String name = localName.isEmpty() ? qName : localName;
        
        if (name.matches("h[1-6]") && "heading".equals(currentType)) {
            String headingText = currentText.toString().trim();
            String lang = detectLanguage(headingText, pendingLanguage);
            chunks.add(new Chunk(headingText, "heading", currentSectionTitle, 
                    (chunkStartPage > 0 ? chunkStartPage : -1), lang));
            int level = Character.isDigit(name.charAt(1)) ? name.charAt(1) - '0' : 1;
            headingStack.push(new HeadingInfo(level, headingText));
            currentSectionTitle = headingText;
            currentText = null;
            currentType = null;
            pendingLanguage = null;
            return;
        }
        
        if (name.equalsIgnoreCase("p") && "paragraph".equals(currentType)) {
            String paragraphText = currentText.toString().trim().replaceAll("\\s+", " ");
            if (!paragraphText.isEmpty()) {
                if (paragraphText.matches("^(\\d+\\.|[A-Za-z]\\.|[-•])\\s+.*")) {
                    // looks like a list item
                    pendingListItems.add(paragraphText);
                } else {
                    flushPendingList();
                    if (pendingParagraphBuffer.length() > 0) pendingParagraphBuffer.append(" ");
                    pendingParagraphBuffer.append(paragraphText);
                    if (paragraphText.matches(".*[.!?][\"'”’)]*\\s*$")) {
                        String full = pendingParagraphBuffer.toString().trim();
                        String lang = detectLanguage(full, pendingLanguage);
                        chunks.add(new Chunk(full, "paragraph", currentSectionTitle,
                                (chunkStartPage > 0 ? chunkStartPage : -1), lang));
                        pendingParagraphBuffer.setLength(0);
                    }
                }
            }
            currentText = null;
            currentType = null;
            pendingLanguage = null;
            return;
        }
        
        if (name.equalsIgnoreCase("li")) {
            if (currentText != null) currentText.append("\n");
            return;
        }
        
        if (name.equalsIgnoreCase("ul") || name.equalsIgnoreCase("ol")) {
            if (!listStack.isEmpty()) listStack.pop();
            if (listStack.isEmpty() && "list".equals(currentType)) {
                String listText = currentText.toString().trim();
                String lang = detectLanguage(listText, null);
                chunks.add(new Chunk(listText, "list", currentSectionTitle, 
                        (chunkStartPage > 0 ? chunkStartPage : -1), lang));
                currentText = null;
                currentType = null;
            }
            return;
        }
        
        if (name.equalsIgnoreCase("table") && "table".equals(currentType)) {
            String tableText = currentText.toString().trim();
            String lang = detectLanguage(tableText, null);
            chunks.add(new Chunk(tableText, "table", currentSectionTitle, 
                    (chunkStartPage > 0 ? chunkStartPage : -1), lang));
            currentText = null;
            currentType = null;
            return;
        }
        
        if ((name.equalsIgnoreCase("caption") || name.equalsIgnoreCase("figcaption")) && insideCaption) {
            insideCaption = false;
            String captionStr = captionText.toString().trim().replaceAll("\\s+", " ");
            String lang = detectLanguage(captionStr, null);
            chunks.add(new Chunk(captionStr, "caption", currentSectionTitle, 
                    (captionStartPage > 0 ? captionStartPage : -1), lang));
            captionText = null;
            captionStartPage = -1;
        }
    }
    
    @Override
    public void characters(char[] ch, int start, int length) {
        String text = new String(ch, start, length);
        if (text.isEmpty()) return;
        if (insideCaption) {
            if (captionText != null) captionText.append(text);
        } else if (currentText != null) {
            currentText.append(text);
        }
    }
    
    @Override
    public void endDocument() {
        flushCurrentChunk();
        flushPendingList();
        if (pendingParagraphBuffer.length() > 0) {
            String full = pendingParagraphBuffer.toString().trim();
            String lang = detectLanguage(full, null);
            chunks.add(new Chunk(full, "paragraph", currentSectionTitle,
                    (chunkStartPage > 0 ? chunkStartPage : -1), lang));
            pendingParagraphBuffer.setLength(0);
        }
    }
    
    private void flushPendingList() {
        if (!pendingListItems.isEmpty()) {
            String listText = String.join("\n", pendingListItems);
            String lang = detectLanguage(listText, null);
            chunks.add(new Chunk(listText, "list", currentSectionTitle,
                    (chunkStartPage > 0 ? chunkStartPage : -1), lang));
            pendingListItems.clear();
        }
    }
    
    private void flushCurrentChunk() {
        if (currentType == null || currentText == null) return;
        String textContent = currentText.toString().trim();
        if (!textContent.isEmpty()) {
            if ("paragraph".equals(currentType)) {
                if (textContent.matches("^(\\d+\\.|[A-Za-z]\\.|[-•])\\s+.*")) {
                    pendingListItems.add(textContent);
                } else {
                    flushPendingList();
                    if (pendingParagraphBuffer.length() > 0) pendingParagraphBuffer.append(" ");
                    pendingParagraphBuffer.append(textContent);
                    if (textContent.matches(".*[.!?][\"'”’)]*\\s*$")) {
                        String full = pendingParagraphBuffer.toString().trim();
                        String lang = detectLanguage(full, pendingLanguage);
                        chunks.add(new Chunk(full, currentType, currentSectionTitle, 
                                (chunkStartPage > 0 ? chunkStartPage : -1), lang));
                        pendingParagraphBuffer.setLength(0);
                    }
                }
            } else {
                String normalized = textContent.replaceAll("\\r?\\n+", " ");
                String lang = detectLanguage(normalized, pendingLanguage);
                chunks.add(new Chunk(normalized, currentType, currentSectionTitle, 
                        (chunkStartPage > 0 ? chunkStartPage : -1), lang));
            }
        }
        currentText = null;
        currentType = null;
        pendingLanguage = null;
    }
    
    private String detectLanguage(String text, String hintedLang) {
        if (hintedLang != null && !hintedLang.isEmpty()) return hintedLang;
        if (text == null || text.isEmpty()) return "";
        return new LanguageIdentifier(text).getLanguage();
    }
    
    public List<Chunk> getChunks() { return chunks; }
    
    private String getLanguageAttribute(Attributes attrs) {
        String lang = attrs.getValue("lang");
        if (lang == null) lang = attrs.getValue("xml:lang");
        return lang;
    }
    
    private String getIndentation(int level) {
        if (level <= 0) return "";
        return "    ".repeat(level);
    }
}

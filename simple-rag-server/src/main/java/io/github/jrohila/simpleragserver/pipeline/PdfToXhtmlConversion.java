package io.github.jrohila.simpleragserver.pipeline;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.TextPosition;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.pdfbox.text.PDFTextStripper;

@Service
public class PdfToXhtmlConversion {

    private static final Logger LOGGER = Logger.getLogger(PdfToXhtmlConversion.class.getName());

    /**
     * Parse the input bytes as a document and return XHTML string produced by
     * Apache Tika.
     */
    public String parseToXhtml(byte[] bytes) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(bytes))) {
            HeadingDetectingStripper stripper = new HeadingDetectingStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());
            stripper.getText(document); // triggers processing
            return stripper.getXhtml();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "PdfToXhtmlConversionService.parseToXhtml: failed to parse bytes to XHTML (PDFBox)", e);
            throw new RuntimeException("Failed to parse PDF to XHTML (PDFBox)", e);
        }
    }

    /**
     * First iteration: log every heading (h1..h6) found in the XHTML to
     * evaluate Tika's structure quality.
     */
    public void logHeadings(byte[] bytes) {
        String xhtml = parseToXhtml(bytes);
        if (xhtml.isEmpty()) {
            LOGGER.info("PdfToXhtmlConversionService.logHeadings: empty XHTML output");
            return;
        }
        Document doc = Jsoup.parse(xhtml, StandardCharsets.UTF_8.name());
        List<Element> headers = doc.select("h1, h2, h3, h4, h5, h6");
        if (headers == null || headers.isEmpty()) {
            LOGGER.info("PdfToXhtmlConversionService.logHeadings: no headings (h1..h6) found");
            return;
        }
        LOGGER.info("PdfToXhtmlConversionService.logHeadings: headings found = " + headers.size());
        for (Element h : headers) {
            String type = h.tagName();
            String text = h.text();
            LOGGER.info(String.format("Heading: type=%s text=%s", type, text));
        }
    }

    /**
     * PDFTextStripper subclass that heuristically detects headings by font size
     * and boldness.
     */
    private static class HeadingDetectingStripper extends PDFTextStripper {

        private enum ListType {
            NONE, UNORDERED, ORDERED
        }

        private static class Bullet {

            final ListType type;
            final String content;

            Bullet(ListType type, String content) {
                this.type = type;
                this.content = content;
            }
        }

        private static class TextChunk {

            String text;
            float fontSize;
            boolean bold;
            float y; // representative baseline Y of the line
            int page; // page number (1-based)

            TextChunk(String text, float fontSize, boolean bold, float y, int page) {
                this.text = text;
                this.fontSize = fontSize;
                this.bold = bold;
                this.y = y;
                this.page = page;
            }
        }

        private final List<TextChunk> chunks = new ArrayList<>();
        private final StringBuilder current = new StringBuilder();
        private float currentFontSize = -1;
        private boolean currentBold = false;
        private float currentY = Float.NaN; // track current line Y
        private int currentPageNo = 0; // track page number
        // Patterns to detect bullet/ordered list markers
        private static final Pattern UNORDERED_RE = Pattern.compile("^\\s*(?:[•◦·▪□■●○‣∙*]|[-–—])\\s+(.*\\S)\\s*$");
        private static final Pattern ORDERED_RE = Pattern.compile(
                "^\\s*(?:\\(?\\d+\\)|\\d+\\.|\\d+\\)|\\(?[a-zA-Z]\\)|[a-zA-Z]\\.|\\(?[ivxlcdmIVXLCDM]+\\)|[ivxlcdmIVXLCDM]+\\.)\\s+(.*\\S)\\s*$");

        HeadingDetectingStripper() throws IOException {
            super();
        }

        @Override
        protected void startPage(org.apache.pdfbox.pdmodel.PDPage page) throws IOException {
            super.startPage(page);
            currentPageNo++;
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            if (textPositions.isEmpty()) {
                return;
            }
            float fontSize = textPositions.get(0).getFontSizeInPt();
            boolean bold = textPositions.get(0).getFont().getName().toLowerCase().contains("bold");
            // compute representative Y for this fragment (average adjusted Y)
            float sumY = 0f;
            for (TextPosition tp : textPositions) {
                sumY += tp.getYDirAdj();
            }
            float avgY = sumY / textPositions.size();
            // If font size or boldness changes, flush current chunk
            if (current.length() > 0 && (fontSize != currentFontSize || bold != currentBold)) {
                chunks.add(new TextChunk(current.toString().trim(), currentFontSize, currentBold, currentY, currentPageNo));
                current.setLength(0);
            }
            currentFontSize = fontSize;
            currentBold = bold;
            currentY = avgY;
            current.append(string);
        }

        @Override
        protected void writeLineSeparator() throws IOException {
            if (current.length() > 0) {
                chunks.add(new TextChunk(current.toString().trim(), currentFontSize, currentBold, currentY, currentPageNo));
                current.setLength(0);
            }
        }

        String getXhtml() {
            if (current.length() > 0) {
                chunks.add(new TextChunk(current.toString().trim(), currentFontSize, currentBold, currentY, currentPageNo));
                current.setLength(0);
            }
            // 1. Find modal (most common) font size for paragraphs
            java.util.Map<Float, Integer> fontFreq = new java.util.HashMap<>();
            for (TextChunk c : chunks) {
                if (c.text.isEmpty()) {
                    continue;
                }
                fontFreq.put(c.fontSize, fontFreq.getOrDefault(c.fontSize, 0) + 1);
            }
            float paraFont = -1;
            int maxCount = 0;
            for (var e : fontFreq.entrySet()) {
                if (e.getValue() > maxCount) {
                    paraFont = e.getKey();
                    maxCount = e.getValue();
                }
            }
            // 2. Collect all unique font sizes above paragraph size, descending
            java.util.Set<Float> headingSizes = new java.util.TreeSet<>((a, b) -> Float.compare(b, a));
            for (Float f : fontFreq.keySet()) {
                if (f > paraFont) {
                    headingSizes.add(f);
                }
            }
            java.util.List<Float> headingList = new java.util.ArrayList<>(headingSizes);
            // 3. Map font size to heading tag (h1, h2, ...), paraFont to p
            java.util.Map<Float, String> fontToTag = new java.util.HashMap<>();
            for (int i = 0; i < headingList.size(); i++) {
                fontToTag.put(headingList.get(i), "h" + (i + 1));
            }
            fontToTag.put(paraFont, "p");

            // 3.5 Compute typical line gap among paragraph lines on same page (median)
            java.util.List<Float> paragraphGaps = new java.util.ArrayList<>();
            for (int i = 0; i < chunks.size() - 1; i++) {
                TextChunk a = chunks.get(i);
                TextChunk b = chunks.get(i + 1);
                String tagA = fontToTag.getOrDefault(a.fontSize, "p");
                String tagB = fontToTag.getOrDefault(b.fontSize, "p");
                if ("p".equals(tagA) && "p".equals(tagB) && a.page == b.page) {
                    float gap = Math.abs(b.y - a.y);
                    if (gap > 0.01f) {
                        paragraphGaps.add(gap);
                    }
                }
            }
            float typicalGap;
            if (!paragraphGaps.isEmpty()) {
                paragraphGaps.sort(Float::compare);
                typicalGap = paragraphGaps.get(paragraphGaps.size() / 2);
            } else {
                // fallback heuristic if we couldn't compute gaps
                typicalGap = (paraFont > 0 ? paraFont : 12f) * 1.2f;
            }
            float emptyRowThreshold = typicalGap * 1.8f; // consider a blank row if gap is significantly larger

            StringBuilder xhtml = new StringBuilder();
            xhtml.append("<html><body>\n");
            String prevTag = null;
            StringBuilder merged = new StringBuilder();
            float prevGroupY = Float.NaN;
            int prevGroupPage = -1;
            boolean inList = false;
            ListType currentListType = ListType.NONE;

            int lastListItemIndex = -1;
            for (int i = 0; i < chunks.size(); i++) {
                TextChunk c = chunks.get(i);
                if (c.text.isEmpty()) {
                    continue;
                }
                String tag = fontToTag.getOrDefault(c.fontSize, "p");
                // Bold, single-line text as heading: assign one level below the nearest real heading
                if (tag.equals("p") && c.bold && !c.text.contains("\n") && c.text.split(" ").length < 12) {
                    int assignedLevel = 1;
                    for (int h = headingList.size() - 1; h >= 0; h--) {
                        if (headingList.get(h) > paraFont) {
                            assignedLevel = h + 2;
                            break;
                        }
                    }
                    if (assignedLevel > 6) {
                        assignedLevel = 6;
                    }
                    tag = "h" + assignedLevel;
                }

                Bullet bullet = ("p".equals(tag) ? parseBullet(c.text) : new Bullet(ListType.NONE, null));
                if (bullet.type != ListType.NONE) {
                    if (prevTag != null && merged.length() > 0) {
                        xhtml.append("<").append(prevTag).append(" page=\"").append(prevGroupPage).append("\">")
                                .append(merged).append("</").append(prevTag).append(">\n");
                        merged.setLength(0);
                        prevTag = null;
                    }
                    if (!inList || currentListType != bullet.type) {
                        if (inList) {
                            xhtml.append(currentListType == ListType.UNORDERED ? "</ul>\n" : "</ol>\n");
                        }
                        xhtml.append(bullet.type == ListType.UNORDERED ? "<ul>\n" : "<ol>\n");
                        inList = true;
                        currentListType = bullet.type;
                    }
                    xhtml.append("<li page=\"").append(c.page).append("\">").append(escapeXml(bullet.content)).append("</li>\n");
                    lastListItemIndex = xhtml.length();
                    prevGroupY = c.y;
                    prevGroupPage = c.page;
                    continue;
                } else if (inList) {
                    // If this is a paragraph and not a bullet, and not a heading, treat as continuation of previous <li>
                    if (lastListItemIndex != -1 && tag.equals("p")) {
                        // Remove the closing </li>\n, append this text, and re-add </li>\n
                        int liCloseIdx = xhtml.lastIndexOf("</li>\n");
                        if (liCloseIdx != -1) {
                            xhtml.delete(liCloseIdx, liCloseIdx + 6);
                            xhtml.append(" ").append(escapeXml(c.text)).append("</li>\n");
                        }
                        continue;
                    } else {
                        xhtml.append(currentListType == ListType.UNORDERED ? "</ul>\n" : "</ol>\n");
                        inList = false;
                        currentListType = ListType.NONE;
                        lastListItemIndex = -1;
                    }
                }

                if (prevTag == null) {
                    prevTag = tag;
                    merged.append(escapeXml(c.text));
                    prevGroupY = c.y;
                    prevGroupPage = c.page;
                } else if (tag.equals(prevTag) && (tag.startsWith("h"))) {
                    // Merge consecutive headings of the same level
                    merged.append(" ").append(escapeXml(c.text));
                } else {
                    xhtml.append("<").append(prevTag).append(" page=\"").append(prevGroupPage).append("\">")
                            .append(merged).append("</").append(prevTag).append(">\n");
                    // If both previous and current are paragraphs on the same page and there's a large vertical gap, insert a <br/>
                    if ("p".equals(prevTag) && "p".equals(tag) && prevGroupPage == c.page) {
                        float gap = Math.abs(c.y - prevGroupY);
                        if (gap > emptyRowThreshold) {
                            xhtml.append("<br/>\n");
                        }
                    }
                    merged.setLength(0);
                    merged.append(escapeXml(c.text));
                    prevTag = tag;
                    prevGroupY = c.y;
                    prevGroupPage = c.page;
                }
            }
            // Write the last merged chunk
            if (merged.length() > 0 && prevTag != null) {
                xhtml.append("<").append(prevTag).append(" page=\"").append(prevGroupPage).append("\">")
                        .append(merged).append("</").append(prevTag).append(">\n");
            }
            // Close any remaining open list
            if (inList) {
                xhtml.append(currentListType == ListType.UNORDERED ? "</ul>\n" : "</ol>\n");
            }
            xhtml.append("</body></html>");
            return xhtml.toString();
        }

        private String escapeXml(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        private Bullet parseBullet(String text) {
            String t = text == null ? "" : text;
            Matcher mu = UNORDERED_RE.matcher(t);
            if (mu.matches()) {
                String content = mu.group(1).trim();
                if (!content.isEmpty()) {
                    return new Bullet(ListType.UNORDERED, content);
                }
            }
            Matcher mo = ORDERED_RE.matcher(t);
            if (mo.matches()) {
                String content = mo.group(1).trim();
                if (!content.isEmpty()) {
                    return new Bullet(ListType.ORDERED, content);
                }
            }
            return new Bullet(ListType.NONE, null);
        }
    }
}

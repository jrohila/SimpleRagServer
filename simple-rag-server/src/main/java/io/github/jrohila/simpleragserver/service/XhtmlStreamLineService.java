package io.github.jrohila.simpleragserver.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service
public class XhtmlStreamLineService {
    /**
     * Merges adjacent <p> elements in the XHTML string if there is no headline or line separator between them
     * and the first paragraph does not end with a terminal punctuation.
     *
     * @param xhtml input XHTML string
     * @return streamlined XHTML string
     */
    public String streamlineParagraphs(String xhtml) {
        Document doc = Jsoup.parse(xhtml, "", org.jsoup.parser.Parser.xmlParser());
        Element body = doc.body();
        if (body == null) return xhtml;

        for (int i = 0; i < body.children().size() - 1; ) {
            Element current = body.child(i);
            Element next = body.child(i + 1);
            if (isParagraph(current) && isParagraph(next)
                    && !hasHeadlineOrSeparatorBetween(body, i, i + 1)
                    && shouldMerge(current, next)) {
                // Merge next into current
                current.appendText(" " + next.text());
                next.remove();
                // Do not increment i, check again in case of further merges
            } else {
                i++;
            }
        }
        return doc.outerHtml();
    }

    private boolean isParagraph(Element el) {
        return el.tagName().equalsIgnoreCase("p");
    }

    private boolean hasHeadlineOrSeparatorBetween(Element body, int idx1, int idx2) {
        for (int i = idx1 + 1; i < idx2; i++) {
            Element el = body.child(i);
            String tag = el.tagName().toLowerCase();
            if (tag.matches("h[1-6]") || tag.equals("br")) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldMerge(Element p1, Element p2) {
        String text1 = p1.text().trim();
        // If text1 ends with terminal punctuation, do not merge
        return !text1.matches(".*[.!?]\s*$");
    }
}

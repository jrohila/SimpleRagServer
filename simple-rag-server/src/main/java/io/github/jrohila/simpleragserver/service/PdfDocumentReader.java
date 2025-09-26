package io.github.jrohila.simpleragserver.service;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ToXMLContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xml.sax.SAXException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class PdfDocumentReader {

    private static final Logger LOGGER = Logger.getLogger(PdfDocumentReader.class.getName());

    /**
     * Parse the input bytes as a document and return XHTML string produced by Apache Tika.
     */
    public String parseToXhtml(byte[] bytes) {
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        ToXMLContentHandler handler = new ToXMLContentHandler();
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            parser.parse(in, handler, metadata, context);
            String xhtml = handler.toString();
            if (xhtml == null) {
                xhtml = "";
            }
            return xhtml;
        } catch (IOException | TikaException | SAXException e) {
            LOGGER.log(Level.SEVERE, "PdfDocumentReader.parseToXhtml: failed to parse bytes to XHTML", e);
            throw new RuntimeException("Failed to parse PDF to XHTML", e);
        }
    }

    /**
     * First iteration: log every heading (h1..h6) found in the XHTML to evaluate Tika's structure quality.
     */
    public void logHeadings(byte[] bytes) {
        String xhtml = parseToXhtml(bytes);
        if (xhtml.isEmpty()) {
            LOGGER.info("PdfDocumentReader.logHeadings: empty XHTML output");
            return;
        }
        Document doc = Jsoup.parse(xhtml, StandardCharsets.UTF_8.name());
        List<Element> headers = doc.select("h1, h2, h3, h4, h5, h6");
        if (headers == null || headers.isEmpty()) {
            LOGGER.info("PdfDocumentReader.logHeadings: no headings (h1..h6) found");
            return;
        }
        LOGGER.info("PdfDocumentReader.logHeadings: headings found = " + headers.size());
        for (Element h : headers) {
            String type = h.tagName();
            String text = h.text();
            LOGGER.info(String.format("Heading: type=%s text=%s", type, text));
        }
    }
}

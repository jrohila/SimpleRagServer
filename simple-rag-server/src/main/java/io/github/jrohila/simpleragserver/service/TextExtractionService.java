package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.dto.Chunk;
import io.github.jrohila.simpleragserver.util.SmartParagraphContentHandler;
import org.apache.tika.langdetect.tika.LanguageIdentifier;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.metadata.Metadata;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.ParseContext;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
public class TextExtractionService {

    private final Tika tika = new Tika();

    public static enum ParagraphExtractionMode {
        PARAGRAPHS_FROM_RAW_TEXT,
        PARAGRAPHS_FROM_XHTML
    }

    /**
     * Extracts text from a file given as a byte array.
     *
     * @param fileBytes the file content as byte array
     * @return extracted text as String
     */
    public String extractText(byte[] fileBytes) {
        try {
            return tika.parseToString(new ByteArrayInputStream(fileBytes));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from file", e);
        }
    }

    /**
     * Extracts paragraphs from a file given as a byte array.
     *
     * @param fileBytes the file content as byte array
     * @param mode
     * @return extracted paragraphs as String[]
     */
    public Chunk[] extractParagraphs(byte[] fileBytes, ParagraphExtractionMode mode) {
        if (mode == ParagraphExtractionMode.PARAGRAPHS_FROM_XHTML) {
            List<Chunk> paras = new ArrayList<>();

            try (var in = new ByteArrayInputStream(fileBytes)) {
                var parser = new AutoDetectParser();
                var handler = new SmartParagraphContentHandler();
                
                parser.parse(in, handler, new Metadata(), new ParseContext());
            
                paras = handler.getChunks();
            } catch (IOException ex) {
                Logger.getLogger(TextExtractionService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SAXException ex) {
                Logger.getLogger(TextExtractionService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TikaException ex) {
                Logger.getLogger(TextExtractionService.class.getName()).log(Level.SEVERE, null, ex);
            }
            return paras.toArray(new Chunk[0]);
        } else {
            String text = extractText(fileBytes);
            // Split on blank lines to get rough paragraphs
            String[] rawParas = text.split("(?:\r?\n){2,}");
            List<Chunk> chunks = new ArrayList<>();
            for (String p : rawParas) {
                String trimmed = p.replaceAll("\\s+", " ").trim();
                if (trimmed.isEmpty()) continue;
                String lang = new LanguageIdentifier(trimmed).getLanguage();
                chunks.add(new Chunk(trimmed, "paragraph", null, -1, lang));
            }
            return chunks.toArray(new Chunk[0]);
        }
    }
}

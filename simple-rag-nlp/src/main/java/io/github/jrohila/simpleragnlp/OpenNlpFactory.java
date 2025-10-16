/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragnlp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opennlp.tools.langdetect.Language;
import opennlp.tools.langdetect.LanguageDetectorME;
import opennlp.tools.langdetect.LanguageDetectorModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

/**
 *
 * @author Jukka
 */
public class OpenNlpFactory {

    private static final Logger log = LoggerFactory.getLogger(OpenNlpFactory.class);

    private static OpenNlpFactory instance;

    private static final String EN_SENT_MODEL_PATH = "/models/sentences/opennlp-en-ud-ewt-sentence-1.3-2.5.4.bin";
    private static final String EN_POS_MODEL_PATH = "/models/pos/opennlp-en-ud-ewt-pos-1.3-2.5.4.bin";

    // Try common language detector model names (adjust to your resources if needed)
    private static final List<String> LANG_MODEL_CANDIDATES = Arrays.asList(
            "/models/lang/langdetect-183.bin",
            "/models/lang/langdetect-187.bin",
            "/models/lang/opennlp-langdetect-183-2.5.4.bin",
            "/models/lang/opennlp-langdetect-187-2.5.4.bin"
    );

    private volatile SentenceDetectorME enSentenceDetector;
    private volatile POSTaggerME enPosTagger;
    private volatile LanguageDetectorME langDetector;

    private OpenNlpFactory() {
        synchronized (this) {
            try (InputStream is = getClass().getResourceAsStream(EN_SENT_MODEL_PATH)) {
                if (is == null) {
                    throw new IllegalStateException("English sentence model not found at " + EN_SENT_MODEL_PATH);
                }
                SentenceModel model = new SentenceModel(is);
                enSentenceDetector = new SentenceDetectorME(model);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load English sentence model: " + e.getMessage(), e);
            }
            // POS model is optional to initialize here; keep lazy if not needed immediately.
        }
        synchronized (this) {
            if (langDetector != null) {
                return;
            }
            String chosenPath = null;
            for (String candidate : LANG_MODEL_CANDIDATES) {
                try (InputStream is = getClass().getResourceAsStream(candidate)) {
                    if (is == null) {
                        continue;
                    }
                    LanguageDetectorModel model = new LanguageDetectorModel(is);
                    langDetector = new LanguageDetectorME(model);
                    chosenPath = candidate;
                    break;
                } catch (IOException e) {
                    // try next
                }
            }
            if (langDetector == null) {
                throw new IllegalStateException("OpenNLP language detector model not found on classpath. Tried: " + LANG_MODEL_CANDIDATES);
            }
            try {
                log.info("OpenNLP language detector initialized (model={})", chosenPath);
            } catch (Exception ignore) {
            }
        }

    }

    public static synchronized OpenNlpFactory getInstance() {
        if (instance == null) {
            instance = new OpenNlpFactory();
        }
        return instance;
    }

    public SentenceDetectorME getEnSentenceDetector() {
        return enSentenceDetector;
    }

    public POSTaggerME getEnPosTagger() {
        return enPosTagger;
    }

    /**
     * Detects language ISO code using OpenNLP's LanguageDetector. Returns "und"
     * if text is null/blank or detector can't decide.
     */
    public String detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return "und";
        }
        Language best = langDetector.predictLanguage(text);
        if (best == null) {
            return "und";
        }
        String code = best.getLang();
        if (code == null || code.isBlank()) {
            return "und";
        }
        return code;
    }
}

package io.github.jrohila.simpleragserver.service;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.apache.tika.language.detect.LanguageConfidence;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
public class NlpService {

    private static final Logger log = LoggerFactory.getLogger(NlpService.class);

    private static final String EN_SENT_MODEL_PATH = "/models/sentences/opennlp-en-ud-ewt-sentence-1.3-2.5.4.bin";

    private final LanguageDetector languageDetector;
    private volatile SentenceDetectorME enSentenceDetector;

    public NlpService() {
        this.languageDetector = new OptimaizeLangDetector().loadModels();
    }

    public String detectLanguage(String text) {
        if (text == null) return "und";
        String normalized = text.strip();
        if (normalized.isEmpty()) return "und";
        LanguageResult result = languageDetector.detect(normalized);
        if (result == null) return "und";
        LanguageConfidence confidence = Optional.ofNullable(result.getConfidence()).orElse(LanguageConfidence.NONE);
        if (confidence == LanguageConfidence.HIGH || confidence == LanguageConfidence.MEDIUM) {
            return result.getLanguage();
        }
        return "und";
    }

    public int countEnglishSentences(String text) {
        if (text == null || text.isBlank()) return 0;
        try {
            SentenceDetectorME detector = getEnglishSentenceDetector();
            String[] sents = detector.sentDetect(text);
            return sents == null ? 0 : sents.length;
        } catch (RuntimeException e) {
            log.warn("Sentence detection failed; returning 0. reason={}", e.getMessage());
            return 0;
        }
    }

    private SentenceDetectorME getEnglishSentenceDetector() {
        SentenceDetectorME local = enSentenceDetector;
        if (local != null) return local;
        synchronized (this) {
            if (enSentenceDetector == null) {
                try (InputStream is = getClass().getResourceAsStream(EN_SENT_MODEL_PATH)) {
                    if (is == null) {
                        throw new IllegalStateException("English sentence model not found at " + EN_SENT_MODEL_PATH);
                    }
                    SentenceModel model = new SentenceModel(is);
                    enSentenceDetector = new SentenceDetectorME(model);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to load English sentence model: " + e.getMessage(), e);
                }
            }
            return enSentenceDetector;
        }
    }
}

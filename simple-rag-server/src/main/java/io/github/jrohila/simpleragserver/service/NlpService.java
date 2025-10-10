package io.github.jrohila.simpleragserver.service;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.apache.tika.language.detect.LanguageConfidence;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
public class NlpService {

    private static final Logger log = LoggerFactory.getLogger(NlpService.class);

    private static final String EN_SENT_MODEL_PATH = "/models/sentences/opennlp-en-ud-ewt-sentence-1.3-2.5.4.bin";
    private static final String EN_POS_MODEL_PATH = "/models/pos/opennlp-en-ud-ewt-pos-1.3-2.5.4.bin";

    private final LanguageDetector languageDetector;
    private volatile SentenceDetectorME enSentenceDetector;
    private volatile POSTaggerME enPosTagger;

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

    /**
     * Tokenize and POS-tag English text.
     * Returns an ordered list of tokens with their POS tags and tag probabilities.
     */
    public List<TaggedToken> tagEnglishPos(String text) {
        List<TaggedToken> result = new ArrayList<>();
        if (text == null) return result;
        String normalized = text.strip();
        if (normalized.isEmpty()) return result;
        try {
            POSTaggerME tagger = getEnglishPosTagger();
            String[] tokens = SimpleTokenizer.INSTANCE.tokenize(normalized);
            if (tokens.length == 0) return result;
            String[] tags = tagger.tag(tokens);
            double[] probs = tagger.probs();
            for (int i = 0; i < tokens.length; i++) {
                double p = (probs != null && probs.length > i) ? probs[i] : Double.NaN;
                result.add(new TaggedToken(tokens[i], tags[i], p));
            }
        } catch (RuntimeException e) {
            log.warn("POS tagging failed; returning empty list. reason={}", e.getMessage());
        }
        return result;
    }

    /**
     * Extract simple candidate terms (noun phrases) from English text using POS tags.
     * Heuristic: sequences of adjectives (JJ*) followed by one or more nouns (NN*),
     * preserving original token text. Duplicates are removed preserving order.
     */
    public List<String> extractCandidateTerms(String text) {
        List<String> terms = new ArrayList<>();
        if (text == null || text.isBlank()) return terms;

        List<TaggedToken> tagged = tagEnglishPos(text);
        if (tagged.isEmpty()) return terms;

        List<String> current = new ArrayList<>();
        boolean seenNoun = false;

        for (TaggedToken tt : tagged) {
            String tag = tt.tag;
            if (isAdjective(tag) || isNoun(tag)) {
                current.add(tt.token);
                if (isNoun(tag)) seenNoun = true;
            } else {
                if (seenNoun && !current.isEmpty()) {
                    terms.add(String.join(" ", current));
                }
                current.clear();
                seenNoun = false;
            }
        }
        if (seenNoun && !current.isEmpty()) {
            terms.add(String.join(" ", current));
        }

        // Also add strong single-word noun terms not already in phrases
        LinkedHashSet<String> unique = new LinkedHashSet<>(terms);
        for (TaggedToken tt : tagged) {
            if (isNoun(tt.tag)) {
                unique.add(tt.token);
            }
        }
        return new ArrayList<>(unique);
    }

    private boolean isNoun(String tag) {
        // Support both UD (NOUN, PROPN) and PTB (NN, NNS, NNP, NNPS)
        if (tag == null) return false;
        return tag.equals("NOUN") || tag.equals("PROPN") || tag.startsWith("NN");
    }

    private boolean isAdjective(String tag) {
        // Support both UD (ADJ) and PTB (JJ, JJR, JJS)
        if (tag == null) return false;
        return tag.equals("ADJ") || tag.equals("JJ") || tag.equals("JJR") || tag.equals("JJS");
    }

    private POSTaggerME getEnglishPosTagger() {
        POSTaggerME local = enPosTagger;
        if (local != null) return local;
        synchronized (this) {
            if (enPosTagger == null) {
                try (InputStream is = getClass().getResourceAsStream(EN_POS_MODEL_PATH)) {
                    if (is == null) {
                        throw new IllegalStateException("English POS model not found at " + EN_POS_MODEL_PATH);
                    }
                    POSModel model = new POSModel(is);
                    enPosTagger = new POSTaggerME(model);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to load English POS model: " + e.getMessage(), e);
                }
            }
            return enPosTagger;
        }
    }

    public static class TaggedToken {
        public final String token;
        public final String tag;
        public final double probability;

        public TaggedToken(String token, String tag, double probability) {
            this.token = token;
            this.tag = tag;
            this.probability = probability;
        }
    }
}

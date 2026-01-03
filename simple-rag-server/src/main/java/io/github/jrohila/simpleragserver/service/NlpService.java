package io.github.jrohila.simpleragserver.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import opennlp.tools.langdetect.Language;
import opennlp.tools.langdetect.LanguageDetectorME;
import opennlp.tools.langdetect.LanguageDetectorModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import opennlp.tools.tokenize.SimpleTokenizer;

@Service
public class NlpService {

    private static final Logger log = LoggerFactory.getLogger(NlpService.class);

    @Value("${nlp.sent-model-path:/models/sentences/opennlp-en-ud-ewt-sentence-1.3-2.5.4.bin}")
    private String enSentModelPath;

    @Value("${nlp.pos-model-path:/models/pos/opennlp-en-ud-ewt-pos-1.3-2.5.4.bin}")
    private String enPosModelPath;

    @Value("${nlp.lang-model-candidates:/models/lang/langdetect-183.bin,/models/lang/langdetect-187.bin,/models/lang/opennlp-langdetect-183-2.5.4.bin,/models/lang/opennlp-langdetect-187-2.5.4.bin}")
    private String langModelCandidatesProperty;

    private volatile SentenceDetectorME enSentenceDetector;
    private volatile POSTaggerME enPosTagger;
    private volatile LanguageDetectorME langDetector;

    public NlpService() {
        initialize();
    }

    @PostConstruct
    public void postConstruct() {
        if (enSentenceDetector == null || langDetector == null) {
            initialize();
        }
    }

    private void initialize() {
        synchronized (this) {
            if (enSentenceDetector == null) {
                try (InputStream is = getClass().getResourceAsStream(enSentModelPath)) {
                    if (is == null) {
                        throw new IllegalStateException("English sentence model not found at " + enSentModelPath);
                    }
                    SentenceModel model = new SentenceModel(is);
                    enSentenceDetector = new SentenceDetectorME(model);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to load English sentence model: " + e.getMessage(), e);
                }
            }
        }

        synchronized (this) {
            if (langDetector != null) {
                return;
            }
            String chosenPath = null;
            List<String> candidates = Arrays.asList(langModelCandidatesProperty.split("\\s*,\\s*"));
            String chosen = null;
            for (String candidate : candidates) {
                try (InputStream is = getClass().getResourceAsStream(candidate)) {
                    if (is == null) continue;
                    LanguageDetectorModel model = new LanguageDetectorModel(is);
                    langDetector = new LanguageDetectorME(model);
                    chosen = candidate;
                    break;
                } catch (IOException e) {
                    // try next
                }
            }

            if (langDetector == null) {
                throw new IllegalStateException("OpenNLP language detector model not found on classpath. Tried: " + candidates);
            }
            try {
                log.info("OpenNLP language detector initialized (model={})", chosenPath);
            } catch (Exception ignore) {
            }
        }
    }

    public SentenceDetectorME getEnSentenceDetector() {
        return enSentenceDetector;
    }

    public POSTaggerME getEnPosTagger() {
        initEnPosTaggerIfNeeded();
        return enPosTagger;
    }

    private void initEnPosTaggerIfNeeded() {
        if (enPosTagger != null) {
            return;
        }
        synchronized (this) {
            if (enPosTagger != null) {
                return;
            }
            try (InputStream is = getClass().getResourceAsStream(enPosModelPath)) {
                if (is == null) {
                    log.warn("English POS model not found on classpath at {}", enPosModelPath);
                    return;
                }
                opennlp.tools.postag.POSModel model = new opennlp.tools.postag.POSModel(is);
                enPosTagger = new POSTaggerME(model);
                log.info("Loaded English POS model from classpath: {}", enPosModelPath);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load English POS model: " + e.getMessage(), e);
            }
        }
    }

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

    /**
     * Extract simple candidate terms (noun phrases) from English text using POS
     * tags. Heuristic: sequences of adjectives (JJ*) followed by one or more
     * nouns (NN*), preserving original token text. Duplicates are removed
     * preserving order.
     */
    public List<String> extractCandidateTerms(String text) {
        return extractTerms(text);
    }

    private List<String> extractTerms(String text) {
        List<String> terms = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return terms;
        }

        List<TaggedToken> tagged = tagEnglishPos(text);
        if (tagged.isEmpty()) {
            return terms;
        }

        List<String> current = new ArrayList<>();
        boolean seenNoun = false;

        for (TaggedToken tt : tagged) {
            String tag = tt.tag;
            if (isAdjective(tag) || isNoun(tag)) {
                current.add(tt.token);
                if (isNoun(tag)) {
                    seenNoun = true;
                }
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

    /**
     * Tokenize and POS-tag English text. Returns an ordered list of tokens with
     * their POS tags and tag probabilities.
     */
    private List<TaggedToken> tagEnglishPos(String text) {
        List<TaggedToken> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        String normalized = text.strip();
        if (normalized.isEmpty()) {
            return result;
        }
        try {
            POSTaggerME tagger = getEnPosTagger();
            String[] tokens = SimpleTokenizer.INSTANCE.tokenize(normalized);
            if (tokens.length == 0) {
                return result;
            }
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

    private static boolean isNoun(String tag) {
        // Support both UD (NOUN, PROPN) and PTB (NN, NNS, NNP, NNPS)
        if (tag == null) {
            return false;
        }
        return tag.equals("NOUN") || tag.equals("PROPN") || tag.startsWith("NN");
    }

    private static boolean isAdjective(String tag) {
        // Support both UD (ADJ) and PTB (JJ, JJR, JJS)
        if (tag == null) {
            return false;
        }
        return tag.equals("ADJ") || tag.equals("JJ") || tag.equals("JJR") || tag.equals("JJS");
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

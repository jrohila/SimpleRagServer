/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragnlp.impl;

import io.github.jrohila.simpleragnlp.OpenNlpFactory;
import io.github.jrohila.simpleragnlp.StanfordCoreNlpFactory;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jukka
 */
public class TermFinderONImpl {

    private static final Logger log = LoggerFactory.getLogger(StanfordCoreNlpFactory.class);

    public static List<String> extractTerms(String text) {
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
    private static List<TaggedToken> tagEnglishPos(String text) {
        List<TaggedToken> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        String normalized = text.strip();
        if (normalized.isEmpty()) {
            return result;
        }
        try {
            POSTaggerME tagger = OpenNlpFactory.getInstance().getEnPosTagger();
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

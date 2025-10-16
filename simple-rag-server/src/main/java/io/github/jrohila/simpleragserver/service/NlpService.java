package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragnlp.OpenNlpFactory;
import io.github.jrohila.simpleragnlp.impl.TermFinderONImpl;
import io.github.jrohila.simpleragnlp.impl.TermFinderSCNImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NlpService {

    public enum NlpEngine {
        OPEN_NLP, STANFORD_CORE_NLP
    }

    private static final Logger log = LoggerFactory.getLogger(NlpService.class);

    public String detectLanguage(String text) {
        return OpenNlpFactory.getInstance().detectLanguage(text);
    }
    
    /**
     * Extract simple candidate terms (noun phrases) from English text using POS
     * tags. Heuristic: sequences of adjectives (JJ*) followed by one or more
     * nouns (NN*), preserving original token text. Duplicates are removed
     * preserving order.
     */
    public List<String> extractCandidateTerms(String text, NlpEngine nlpEngine) {
        if (NlpEngine.OPEN_NLP.equals(nlpEngine)) {
            return TermFinderONImpl.extractTerms(text);
        } else {
            return TermFinderSCNImpl.extractTerms(text);
        }
    }

}

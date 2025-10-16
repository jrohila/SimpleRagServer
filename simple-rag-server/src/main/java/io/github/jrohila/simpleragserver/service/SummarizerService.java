package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.util.TextCleaner;
import opennlp.summarization.Summarizer;
import opennlp.summarization.meta.MetaSummarizer;
import opennlp.summarization.preprocess.DefaultDocProcessor;
import opennlp.summarization.lexicalchaining.NounPOSTagger;
import opennlp.summarization.lexicalchaining.LexicalChainingSummarizer;
import opennlp.summarization.textrank.TextRankSummarizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SummarizerService {

    public enum Method { META, LEXICAL_CHAINING, TEXT_RANK }

    private final DefaultDocProcessor docProcessor;
    private final NounPOSTagger posTagger;

    private final Summarizer metaSummarizer;
    private final Summarizer lexicalChainingSummarizer;
    private final Summarizer textRankSummarizer;

    // Default method if none specified
    private final Method defaultMethod;

    public SummarizerService(
            @Value("${summarizer.language:en}") String languageCode,
            @Value("${summarizer.method:META}") String defaultMethodStr) {
        try {
            this.docProcessor = new DefaultDocProcessor(languageCode);
            this.posTagger = new NounPOSTagger(languageCode);
            this.metaSummarizer = new MetaSummarizer(docProcessor, posTagger);
            this.lexicalChainingSummarizer = new LexicalChainingSummarizer(docProcessor, posTagger);
            this.textRankSummarizer = new TextRankSummarizer(docProcessor);
            Method parsed;
            try {
                parsed = Method.valueOf(defaultMethodStr.trim().toUpperCase());
            } catch (Exception ignore) {
                parsed = Method.META;
            }
            this.defaultMethod = parsed;
        } catch (Throwable e) {
            String hint = "Ensure opennlp model artifacts for '" + languageCode + "' are on the classpath (opennlp-models-pos-" + languageCode + ", opennlp-models-sentdetect-" + languageCode + ") and opennlp-tools versions align across modules.";
            throw new IllegalStateException("Failed to initialize summarizer (language='" + languageCode + "). " + hint, e);
        }
    }

    /**
     * Summarize the given text, capping approximately by word count.
     * Note: Upstream API expects maxWords; pass -1 to summarize fully.
     */
    public String summarize(String text, int maxWords) {
        return summarize(text, maxWords, defaultMethod);
    }

    public String summarize(String text, int maxWords, Method method) {
        if (text == null || text.isBlank()) return "";
        String cleaned = TextCleaner.clean(text);
        if (cleaned.isBlank()) return "";
        Summarizer s = switch (method) {
            case META -> metaSummarizer;
            case LEXICAL_CHAINING -> lexicalChainingSummarizer;
            case TEXT_RANK -> textRankSummarizer;
        };
        return s.summarize(cleaned, maxWords);
    }
}

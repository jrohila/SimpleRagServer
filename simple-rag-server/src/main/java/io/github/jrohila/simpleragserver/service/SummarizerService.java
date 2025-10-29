package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.util.TextCleaner;
import opennlp.summarization.Summarizer;
import opennlp.summarization.meta.MetaSummarizer;
import opennlp.summarization.preprocess.DefaultDocProcessor;
import opennlp.summarization.lexicalchaining.NounPOSTagger;
import opennlp.summarization.lexicalchaining.LexicalChainingSummarizer;
import opennlp.summarization.textrank.TextRankSummarizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SummarizerService {

    private static final Logger log = LoggerFactory.getLogger(SummarizerService.class);

    public enum Method {
        META, LEXICAL_CHAINING, TEXT_RANK, BART
    }

    private final DefaultDocProcessor docProcessor;
    private final NounPOSTagger posTagger;

    private final Summarizer metaSummarizer;
    private final Summarizer lexicalChainingSummarizer;
    private final Summarizer textRankSummarizer;

    // Default method if none specified
    private final Method defaultMethod;

    private final String textGenSummarizerUrl;
    private final HttpClient httpClient;

    @Autowired
    public SummarizerService(
            @Value("${summarizer.language:en}") String languageCode,
            @Value("${summarizer.method:META}") String defaultMethodStr,
            @Value("${textgen.summarizer.url:http://localhost:8000/generate}") String textGenSummarizerUrl) {
        this.textGenSummarizerUrl = textGenSummarizerUrl;
        this.httpClient = HttpClient.newBuilder().build();
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
     * Summarize the given text, capping approximately by word count. Note:
     * Upstream API expects maxWords; pass -1 to summarize fully.
     */
    public String summarize(String text, int maxWords) {
        return summarize(text, maxWords, defaultMethod);
    }

    public String summarize(String text, int maxWords, Method method) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = TextCleaner.clean(text);
        if (cleaned.isBlank()) {
            return "";
        }
        if (method == Method.BART) { 
            try {
                // T5 expects a task prefix for summarization
                String t5Input = "summarize: " + cleaned;
                String json = "{\"inputs\": " + escapeJson(t5Input) + "}";
                log.info("TGI T5 summarizer request to {} with payload: {}", textGenSummarizerUrl, json);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(textGenSummarizerUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                        .build();
                log.info("T5 summarizer HTTP request: method=POST, url={}, headers={}, body={}",
                        textGenSummarizerUrl, request.headers().map(), json);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("TGI summarizer HTTP response: status={}, headers={}, body={}",
                        response.statusCode(), response.headers().map(), response.body());
                if (response.statusCode() == 200) {
                    // TGI returns {\"generated_text\": \"...\"} or a list of such objects
                    String body = response.body();
                    int idx = body.indexOf(":");
                    if (idx > 0) {
                        int start = body.indexOf('"', idx);
                        int end = body.lastIndexOf('"');
                        if (start >= 0 && end > start) {
                            return body.substring(start + 1, end);
                        }
                    }
                    return body;
                } else {
                    return "[TGI summarizer error: HTTP " + response.statusCode() + "]";
                }
            } catch (Exception e) {
                log.error("TGI summarizer request failed", e);
                return "[T5 summarizer error: " + e.getMessage() + "]";
            }
        }
        Summarizer s = switch (method) {
            case META ->
                metaSummarizer;
            case LEXICAL_CHAINING ->
                lexicalChainingSummarizer;
            case TEXT_RANK ->
                textRankSummarizer;
            default ->
                metaSummarizer;
        };
        return s.summarize(cleaned, maxWords);
    }

    private static String escapeJson(String text) {
        // Simple JSON string escaper for double quotes and backslashes
        return '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}

package io.github.jrohila.simpleragserver.client;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Ollama implementation of EmbeddingClient using langchain4j.
 */
@Component("ollamaEmbeddingClient")
public class OllamaEmbeddingClient implements EmbeddingClient {
    
    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingClient.class);
    
    private final String baseUrl;
    private final String defaultModel;
    private final Duration timeout;
    private final Integer dimension;
    
    public OllamaEmbeddingClient(
            @Value("${llm.ollama.baseUrl:http://localhost:11434}") String baseUrl,
            @Value("${llm.ollama.embedding.model:embeddinggemma:300m}") String defaultModel,
            @Value("${llm.ollama.embedding.timeout:60}") int timeoutSeconds,
            @Value("${llm.ollama.embedding.dimension:768}") Integer dimension) {
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.dimension = dimension;
        log.info("Initialized OllamaEmbeddingClient with model: {}, dimension: {}", defaultModel, dimension);
    }
    
    @Override
    public float[] embed(String text) {
        log.debug("Generating embeddings for text of length: {}", text.length());
        
        EmbeddingModel model = OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(defaultModel)
                .timeout(timeout)
                .build();
        
        Response<Embedding> response = model.embed(text);
        Embedding embedding = response.content();
        
        float[] vector = embedding.vector();
        log.debug("Generated embedding with dimension: {}", vector.length);
        
        return vector;
    }
    
    @Override
    public String getProviderName() {
        return "ollama";
    }
    
    @Override
    public int getDimension() {
        return dimension;
    }
}

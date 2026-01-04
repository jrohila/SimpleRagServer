package io.github.jrohila.simpleragserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Ollama models to ensure GraalVM compatibility.
 * This configures Jackson ObjectMapper to work with PropertyNamingStrategies in native images.
 */
@Configuration
public class OllamaConfig {

    /**
     * Custom ObjectMapper that uses SNAKE_CASE naming for Ollama API communication.
     * Note: NOT marked as @Primary to avoid affecting controller JSON serialization.
     */
    @Bean
    @Qualifier("ollamaObjectMapper")
    public ObjectMapper ollamaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Use the SNAKE_CASE static instance for Ollama API compatibility
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }
}

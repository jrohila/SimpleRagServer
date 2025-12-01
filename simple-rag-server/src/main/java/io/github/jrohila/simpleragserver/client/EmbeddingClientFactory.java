package io.github.jrohila.simpleragserver.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and managing EmbeddingClient instances.
 * Supports multiple providers through Spring's dependency injection.
 */
@Component
public class EmbeddingClientFactory {
    
    private static final Logger log = LoggerFactory.getLogger(EmbeddingClientFactory.class);
    
    private final ApplicationContext applicationContext;
    private final String defaultProvider;
    private final Map<String, EmbeddingClient> clientCache = new ConcurrentHashMap<>();
    
    public EmbeddingClientFactory(
            ApplicationContext applicationContext,
            @Value("${llm.defaultProvider:ollama}") String defaultProvider) {
        this.applicationContext = applicationContext;
        this.defaultProvider = defaultProvider;
        log.info("Initialized EmbeddingClientFactory with default provider: {}", defaultProvider);
    }
    
    /**
     * Get an embedding client for the specified provider.
     * 
     * @param provider The provider name (e.g., "ollama", "openai")
     * @return The embedding client for the specified provider
     * @throws IllegalArgumentException if the provider is not supported
     */
    public EmbeddingClient getClient(String provider) {
        return clientCache.computeIfAbsent(provider, p -> {
            String beanName = p.toLowerCase() + "EmbeddingClient";
            try {
                EmbeddingClient client = applicationContext.getBean(beanName, EmbeddingClient.class);
                log.info("Created embedding client for provider: {}", p);
                return client;
            } catch (Exception e) {
                log.error("Failed to create embedding client for provider: {}", p, e);
                throw new IllegalArgumentException("Unsupported embedding provider: " + p, e);
            }
        });
    }
    
    /**
     * Get the default embedding client.
     * 
     * @return The default embedding client
     */
    public EmbeddingClient getDefaultClient() {
        return getClient(defaultProvider);
    }
}

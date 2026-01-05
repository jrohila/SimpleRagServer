package io.github.jrohila.simpleragserver.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.BeanContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and managing EmbeddingClient instances.
 * Supports multiple providers through Micronaut's dependency injection.
 */
@Singleton
public class EmbeddingClientFactory {
    
    private static final Logger log = LoggerFactory.getLogger(EmbeddingClientFactory.class);
    
    private final BeanContext beanContext;
    private final String defaultProvider;
    private final Map<String, EmbeddingClient> clientCache = new ConcurrentHashMap<>();
    
    public EmbeddingClientFactory(
            BeanContext beanContext,
            @Property(name = "llm.defaultProvider", defaultValue = "ollama") String defaultProvider) {
        this.beanContext = beanContext;
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
                EmbeddingClient client = beanContext.getBean(EmbeddingClient.class, Qualifiers.byName(beanName));
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

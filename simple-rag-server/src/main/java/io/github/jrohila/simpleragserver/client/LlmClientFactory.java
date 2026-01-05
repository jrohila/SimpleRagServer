package io.github.jrohila.simpleragserver.client;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.BeanContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating LLM client instances based on provider name.
 * Uses Micronaut's dependency injection to locate provider implementations.
 */
@Singleton
public class LlmClientFactory {
    
    private final BeanContext beanContext;
    private final String defaultProvider;
    private final Map<String, LlmClient> clientCache = new ConcurrentHashMap<>();
    
    public LlmClientFactory(
            BeanContext beanContext,
            @Property(name = "llm.defaultProvider", defaultValue = "ollama") String defaultProvider) {
        this.beanContext = beanContext;
        this.defaultProvider = defaultProvider;
    }
    
    /**
     * Get an LLM client for the specified provider.
     * 
     * @param provider Provider name (e.g., "ollama", "openai", "gemini")
     * @return LlmClient implementation for that provider
     * @throws IllegalArgumentException if provider is not supported
     */
    public LlmClient getClient(String provider) {
        if (provider == null || provider.isBlank()) {
            provider = defaultProvider;
        }
        
        return clientCache.computeIfAbsent(provider.toLowerCase(), this::createClient);
    }
    
    /**
     * Get the default LLM client based on configuration.
     */
    public LlmClient getDefaultClient() {
        return getClient(defaultProvider);
    }
    
    private LlmClient createClient(String provider) {
        String beanName = provider + "LlmClient";
        try {
            return beanContext.getBean(LlmClient.class, Qualifiers.byName(beanName));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "No LLM client implementation found for provider: " + provider + 
                " (looked for bean: " + beanName + ")", e);
        }
    }
}

package io.github.jrohila.simpleragserver.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating LLM client instances based on provider name.
 * Uses Spring's dependency injection to locate provider implementations.
 */
@Component
public class LlmClientFactory {
    
    private final ApplicationContext applicationContext;
    private final String defaultProvider;
    private final Map<String, LlmClient> clientCache = new ConcurrentHashMap<>();
    
    @Autowired
    public LlmClientFactory(
            ApplicationContext applicationContext,
            @Value("${llm.defaultProvider:ollama}") String defaultProvider) {
        this.applicationContext = applicationContext;
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
            return applicationContext.getBean(beanName, LlmClient.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "No LLM client implementation found for provider: " + provider + 
                " (looked for bean: " + beanName + ")", e);
        }
    }
}

package io.github.jrohila.simpleragserver.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface for embedding client implementations.
 * Supports generating embeddings from text using various providers.
 */
public interface EmbeddingClient {
    
    /**
     * Generate embeddings for the given text.
     * 
     * @param text The text to generate embeddings for
     * @return Embeddings as a primitive float array
     */
    float[] embed(String text);
    
    /**
     * Generate embeddings for the given text.
     * Convenience method that returns embeddings as a List of Float objects.
     * 
     * @param text The text to generate embeddings for
     * @return Embeddings as a List of Float
     */
    default List<Float> embedAsList(String text) {
        float[] embeddings = embed(text);
        List<Float> result = new ArrayList<>(embeddings.length);
        for (float value : embeddings) {
            result.add(value);
        }
        return result;
    }
    
    /**
     * Get the provider name (e.g., "ollama", "openai", "gemini")
     */
    String getProviderName();
    
    /**
     * Get the embedding dimension size for this model
     */
    int getDimension();
}

package io.github.jrohila.simpleragserver.client;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for LLM client implementations.
 * Supports both request/response and streaming modes.
 */
public interface LlmClient {
    
    /**
     * Send a synchronous chat request and get a complete response.
     * 
     * @param messages List of chat messages
     * @param options Request options (temperature, maxTokens, etc.)
     * @return Response containing the assistant's reply
     */
    Response<String> chat(List<ChatMessage> messages, LlmRequestOptions options);
    
    /**
     * Send a synchronous chat request with a simple string message.
     * 
     * @param userMessage The user's message as a string
     * @param options Request options (temperature, maxTokens, etc.)
     * @return Response containing the assistant's reply
     */
    default Response<String> chat(String userMessage, LlmRequestOptions options) {
        return chat(List.of(UserMessage.from(userMessage)), options);
    }
    
    /**
     * Send a streaming chat request where response chunks are delivered via callback.
     * 
     * @param messages List of chat messages
     * @param options Request options (temperature, maxTokens, etc.)
     * @param streamHandler Consumer that receives each response chunk
     * @param onComplete Runnable called when streaming completes successfully
     * @param onError Consumer called if an error occurs during streaming
     */
    void streamChat(List<ChatMessage> messages, LlmRequestOptions options, 
                    Consumer<String> streamHandler, Runnable onComplete, Consumer<Throwable> onError);
    
    /**
     * Send a streaming chat request with a simple string message.
     * 
     * @param userMessage The user's message as a string
     * @param options Request options (temperature, maxTokens, etc.)
     * @param streamHandler Consumer that receives each response chunk
     * @param onComplete Runnable called when streaming completes successfully
     * @param onError Consumer called if an error occurs during streaming
     */
    default void streamChat(String userMessage, LlmRequestOptions options,
                           Consumer<String> streamHandler, Runnable onComplete, Consumer<Throwable> onError) {
        streamChat(List.of(UserMessage.from(userMessage)), options, streamHandler, onComplete, onError);
    }
    
    /**
     * Get the provider name (e.g., "ollama", "openai", "gemini")
     */
    String getProviderName();
}

package io.github.jrohila.simpleragserver.client;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ollama implementation of LlmClient using langchain4j.
 */
@Component("ollamaLlmClient")
public class OllamaLlmClient implements LlmClient {
    
    private static final Logger log = LoggerFactory.getLogger(OllamaLlmClient.class);
    
    private final String baseUrl;
    private final String defaultModel;
    private final Duration timeout;
    
    public OllamaLlmClient(
            @Value("${llm.ollama.baseUrl:http://localhost:11434}") String baseUrl,
            @Value("${llm.ollama.model:llama3.2}") String defaultModel,
            @Value("${llm.ollama.timeout:300}") int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }
    
    @Override
    public Response<String> chat(List<ChatMessage> messages, LlmRequestOptions options) {
        ChatModel model = buildChatModel(options);
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .build();
        ChatResponse response = model.chat(request);
        return Response.from(
                response.aiMessage().text(),
                response.tokenUsage(),
                response.finishReason()
        );
    }
    
    @Override
    public void streamChat(List<ChatMessage> messages, LlmRequestOptions options, 
                          Consumer<String> streamHandler, Runnable onComplete, Consumer<Throwable> onError) {
        log.info("Starting streaming chat with {} messages", messages.size());
        StreamingChatModel model = buildStreamingChatModel(options);
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .build();
        
        model.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (partialResponse != null && !partialResponse.isEmpty()) {
                    log.debug("Received token: '{}'", partialResponse);
                    streamHandler.accept(partialResponse);
                }
            }
            
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                log.info("Streaming complete. Total tokens: {}", 
                    completeResponse.tokenUsage() != null ? completeResponse.tokenUsage().totalTokenCount() : "unknown");
                if (onComplete != null) {
                    onComplete.run();
                }
            }
            
            @Override
            public void onError(Throwable error) {
                log.error("Streaming error", error);
                if (onError != null) {
                    onError.accept(error);
                } else {
                    throw new RuntimeException("Streaming chat failed", error);
                }
            }
        });
        log.info("Streaming chat handler registered");
    }
    
    @Override
    public String getProviderName() {
        return "ollama";
    }
    
    private ChatModel buildChatModel(LlmRequestOptions options) {
        OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(options.getModel() != null ? options.getModel() : defaultModel)
                .timeout(timeout);
        
        if (options.getTemperature() != null) {
            builder.temperature(options.getTemperature());
        }
        if (options.getMaxTokens() != null) {
            builder.numPredict(options.getMaxTokens());
        }
        if (options.getTopP() != null) {
            builder.topP(options.getTopP());
        }
        if (options.getTopK() != null) {
            builder.topK(options.getTopK());
        }
        if (options.getStopSequences() != null && !options.getStopSequences().isEmpty()) {
            builder.stop(options.getStopSequences());
        }
        
        return builder.build();
    }
    
    private StreamingChatModel buildStreamingChatModel(LlmRequestOptions options) {
        OllamaStreamingChatModel.OllamaStreamingChatModelBuilder builder = OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(options != null && options.getModel() != null ? options.getModel() : defaultModel)
                .timeout(timeout);
        
        if (options != null) {
            if (options.getTemperature() != null) {
                builder.temperature(options.getTemperature());
            }
            if (options.getMaxTokens() != null) {
                builder.numPredict(options.getMaxTokens());
            }
            if (options.getTopP() != null) {
                builder.topP(options.getTopP());
            }
            if (options.getTopK() != null) {
                builder.topK(options.getTopK());
            }
            if (options.getStopSequences() != null && !options.getStopSequences().isEmpty()) {
                builder.stop(options.getStopSequences());
            }
        }
        
        return builder.build();
    }
}

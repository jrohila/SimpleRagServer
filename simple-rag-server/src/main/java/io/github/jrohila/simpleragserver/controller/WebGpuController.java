package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.chat.pipeline.ContextAdditionPipe;
import io.github.jrohila.simpleragserver.controller.dto.MessageDto;
import io.github.jrohila.simpleragserver.domain.ChatEntity;
import io.github.jrohila.simpleragserver.repository.ChatManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/webgpu")
@RequiredArgsConstructor
public class WebGpuController {

    private final ContextAdditionPipe contextAdditionPipe;
    private final ChatManagerService chatManagerService;

    /**
     * Request body for processing messages before WebGPU LLM
     */
    public record ProcessMessagesRequest(
            String publicName,
            List<MessageDto> messages,
            int maxContextLength,
            int completionLength,
            int headroomLength
    ) {}

    /**
     * Process message list before sending to WebGPU LLM.
     * Adds RAG context using ContextAdditionPipe if publicName is provided.
     *
     * @param request Contains publicName and list of messages from browser
     * @return Optimized message list with RAG context added
     */
    @PostMapping("/process-messages")
    public ResponseEntity<List<MessageDto>> processMessageListBeforeLLM(
            @RequestBody ProcessMessagesRequest request
    ) {
        log.info("Processing messages for WebGPU LLM. PublicName: {}, Message count: {}",
                request.publicName(), request.messages().size());

        try {
            // Validate input
            if (request.messages() == null || request.messages().isEmpty()) {
                log.warn("Empty message list provided");
                return ResponseEntity.badRequest().build();
            }

            // Get the chat entity if publicName is provided
            ChatEntity chatEntity = null;
            if (request.publicName() != null && !request.publicName().isEmpty()) {
                var chatEntityOpt = chatManagerService.getByPublicName(request.publicName());
                
                if (!chatEntityOpt.isPresent()) {
                    log.warn("Chat not found with publicName: {}", request.publicName());
                    return ResponseEntity.notFound().build();
                }
                chatEntity = chatEntityOpt.get();
            }

            // Convert MessageDto to Spring AI Message format
            List<Message> springMessages = request.messages().stream()
                    .map(dto -> {
                        String content = dto.getContent();
                        return switch (dto.getRole().toLowerCase()) {
                            case "system" -> new SystemMessage(content);
                            case "user" -> new UserMessage(content);
                            case "assistant" -> new AssistantMessage(content);
                            default -> throw new IllegalArgumentException("Invalid role: " + dto.getRole());
                        };
                    })
                    .collect(Collectors.toList());

            // Process messages through ContextAdditionPipe if chat entity exists
            List<Message> processedMessages = springMessages;

            if (chatEntity != null) {
                try {
                    // Use ContextAdditionPipe to add RAG context with client-specified limits
                    Pair<ContextAdditionPipe.OperationResult, List<Message>> result = 
                            contextAdditionPipe.process(springMessages, chatEntity, 
                                    request.maxContextLength(), 
                                    request.completionLength(), 
                                    request.headroomLength());
                    
                    processedMessages = result.getRight();

                    log.info("RAG context processing complete for chat: {}, result: {}", 
                            request.publicName(), result.getLeft());
                } catch (Exception e) {
                    log.error("Failed to add RAG context, proceeding without it", e);
                }
            }

            // Ensure system message exists at the beginning
            String systemPrompt = chatEntity != null && chatEntity.getDefaultSystemPrompt() != null
                    ? chatEntity.getDefaultSystemPrompt()
                    : "You are a helpful assistant.";

            boolean hasSystemMessage = processedMessages.stream()
                    .anyMatch(msg -> msg.getMessageType() == MessageType.SYSTEM);

            if (!hasSystemMessage) {
                List<Message> withSystem = new ArrayList<>();
                withSystem.add(new SystemMessage(systemPrompt));
                withSystem.addAll(processedMessages);
                processedMessages = withSystem;
            }

            // Convert back to DTOs
            List<MessageDto> outputMessages = processedMessages.stream()
                    .map(msg -> {
                        String role = msg.getMessageType().getValue();
                        String content = msg.getText();
                        return new MessageDto(role, content);
                    })
                    .collect(Collectors.toList());

            log.info("Message processing complete. Output messages: {}", outputMessages.size());

            return ResponseEntity.ok(outputMessages);

        } catch (Exception e) {
            log.error("Error processing messages for WebGPU LLM", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

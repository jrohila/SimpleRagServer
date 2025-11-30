package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.pipeline.ContextAdditionPipe;
import io.github.jrohila.simpleragserver.pipeline.MessageListPreProcessPipe;
import io.github.jrohila.simpleragserver.domain.ChatEntity;
import io.github.jrohila.simpleragserver.dto.MessageDTO;
import io.github.jrohila.simpleragserver.repository.ChatManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/webgpu")
@RequiredArgsConstructor
public class WebGpuController {

    private final MessageListPreProcessPipe messageListPreProcessPipe;
    private final ContextAdditionPipe contextAdditionPipe;
    private final ChatManagerService chatManagerService;

    /**
     * Request body for processing messages before WebGPU LLM
     */
    public record ProcessMessagesRequest(
            String publicName,
            List<MessageDTO> messages,
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
    public ResponseEntity<List<MessageDTO>> processMessageListBeforeLLM(
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

            // Process messages through ContextAdditionPipe if chat entity exists
            List<MessageDTO> processedMessages = this.messageListPreProcessPipe.process(request.messages(), chatEntity);
                    
            if (chatEntity != null) {
                try {
                    // Use ContextAdditionPipe to add RAG context with client-specified limits
                    Pair<ContextAdditionPipe.OperationResult, List<MessageDTO>> result = 
                            contextAdditionPipe.process(processedMessages, chatEntity, 
                                    request.maxContextLength(), 
                                    request.completionLength(), 
                                    request.headroomLength(),
                                    ContextAdditionPipe.ModifyChatHistory.DROP_ALL);
                    
                    processedMessages = result.getRight();

                    log.info("RAG context processing complete for chat: {}, result: {}", 
                            request.publicName(), result.getLeft());
                } catch (Exception e) {
                    log.error("Failed to add RAG context, proceeding without it", e);
                }
            }

            // Convert back to DTOs
            log.info("Message processing complete. Output messages: {}", processedMessages.size());

            return ResponseEntity.ok(processedMessages);

        } catch (Exception e) {
            log.error("Error processing messages for WebGPU LLM", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

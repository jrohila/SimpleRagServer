/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.pipeline;

import io.github.jrohila.simpleragserver.domain.ChatEntity;
import io.github.jrohila.simpleragserver.dto.MessageDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 *
 * @author Jukka
 */
@Component
public class MessageListPreProcessPipe {
    
    public List<MessageDTO> process(List<MessageDTO> messages, ChatEntity chatEntity) {
        List<MessageDTO> processed = new ArrayList<>();

        if (chatEntity.isOverrideSystemMessage()) {
            StringBuilder sb = new StringBuilder();
            sb.append(chatEntity.getDefaultSystemPrompt());
            sb.append(" \n ");
            sb.append(chatEntity.getDefaultSystemPromptAppend());

            Integer maxTokens = null;
            if (chatEntity.getLlmConfig() != null) {
                maxTokens = chatEntity.getLlmConfig().getMaxNewTokens();
            }
            if (maxTokens == null && chatEntity != null && chatEntity.getLlmConfig() != null) {
                maxTokens = chatEntity.getLlmConfig().getMaxNewTokens();
            }
            if (maxTokens != null) {
                sb.append(" \n ");
                sb.append(String.format("You have up to %d tokens available for your response. Be concise and avoid unnecessary repetition.", maxTokens));
            }
            processed.add(new MessageDTO(MessageDTO.Role.SYSTEM, sb.toString()));
        }
        for (MessageDTO message : messages) {
            if (MessageDTO.Role.ASSISTANT.equals(message.getRole())) {
                if (!chatEntity.isOverrideAssistantMessage()) {
                    processed.add(message);
                } else {
                    processed.add(message); // If I override, then what does it mean? compress?
                }
            } else {
                if (MessageDTO.Role.SYSTEM.equals(message.getRole())) {
                    if (!chatEntity.isOverrideSystemMessage()) {
                        processed.add(message);
                    }
                } else {
                    if (MessageDTO.Role.USER.equals(message.getRole())) {
                        processed.add(message);
                    }
                }
            }
        }

        return processed;
    }

}

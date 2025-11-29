/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.chat.pipeline;

import io.github.jrohila.simpleragserver.dto.OpenAiChatRequestDTO;
import io.github.jrohila.simpleragserver.domain.ChatEntity;
import io.github.jrohila.simpleragserver.dto.MessageDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/**
 *
 * @author Jukka
 */
@Component
public class MessageListPreProcessPipe {

    public List<Message> transform(OpenAiChatRequestDTO request) {
        List<Message> messages = new ArrayList<>();
        if (request.getMessages() != null) {
            for (MessageDTO m : request.getMessages()) {
                if (m.getRole() == null) {
                    continue;
                }
                switch (m.getRole()) {
                    case SYSTEM -> messages.add(new SystemMessage(m.getContentAsString()));
                    case USER -> messages.add(new UserMessage(m.getContentAsString()));
                    case ASSISTANT -> messages.add(new AssistantMessage(m.getContentAsString()));
                    case TOOL -> messages.add(new UserMessage(m.getContentAsString()));
                    default -> messages.add(new UserMessage(m.getContentAsString()));
                }
            }
        }
        return messages;
    }

    public List<Message> process(List<Message> messages, ChatEntity chatEntity) {
        List<Message> processed = new ArrayList<>();

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
            processed.add(new SystemMessage(sb.toString()));
        }
        for (Message message : messages) {
            if (MessageType.ASSISTANT.equals(message.getMessageType())) {
                if (!chatEntity.isOverrideAssistantMessage()) {
                    processed.add(message);
                } else {
                    processed.add(message); // If I override, then what does it mean? compress?
                }
            } else {
                if (MessageType.SYSTEM.equals(message.getMessageType())) {
                    if (!chatEntity.isOverrideSystemMessage()) {
                        processed.add(message);
                    }
                } else {
                    if (MessageType.USER.equals(message.getMessageType())) {
                        processed.add(message);
                    }
                }
            }
        }

        return processed;
    }

}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.chat.pipeline;

import io.github.jrohila.simpleragserver.chat.OpenAiChatRequest;
import io.github.jrohila.simpleragserver.domain.ChatEntity;
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

    public List<Message> transform(OpenAiChatRequest request) {
        List<Message> messages = new ArrayList<>();
        if (request.getMessages() != null) {
            for (OpenAiChatRequest.Message m : request.getMessages()) {
                if (m.getRole() == null) {
                    continue;
                }
                switch (m.getRole()) {
                    case "system" -> {
                        messages.add(new SystemMessage(m.getContentAsString()));
                    }
                    case "user" ->
                        messages.add(new UserMessage(m.getContentAsString()));
                    case "assistant" -> {
                        messages.add(new AssistantMessage(m.getContentAsString()));
                    }
                    default ->
                        messages.add(new UserMessage(m.getContentAsString()));
                }
            }
        }
        return messages;
    }

    public List<Message> process(List<Message> messages, ChatEntity chatEntity) {
        List<Message> processed = new ArrayList<>();

        if (chatEntity.isOverrideSystemMessage()) {
            processed.add(new SystemMessage(chatEntity.getDefaultSystemPrompt() + "\n" + chatEntity.getDefaultSystemPromptAppend()));
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

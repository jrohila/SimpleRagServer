package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.ChatEntity;
import io.github.jrohila.simpleragserver.repository.ChatManagerService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/api/chats")
public class ChatManagerController {

    private static final Logger log = LoggerFactory.getLogger(ChatManagerController.class);
    private final ChatManagerService chatManagerService;

    public ChatManagerController(ChatManagerService chatManagerService) {
        this.chatManagerService = chatManagerService;
    }

    @Post
    public HttpResponse<ChatEntity> create(@Body ChatEntity chat) {
        log.info("Received request to create chat: {}", chat);
        ChatEntity created = chatManagerService.create(chat);
        log.info("Chat created: {}", created);
        return HttpResponse.ok(created);
    }

    @Get("/{id}")
    public HttpResponse<ChatEntity> getById(@PathVariable String id) {
        log.info("Received request to get chat by id: {}", id);
        Optional<ChatEntity> chat = chatManagerService.getById(id);
        if (chat.isPresent()) {
            log.info("Chat found for id: {}", id);
        } else {
            log.info("No chat found for id: {}", id);
        }
        return chat.map(HttpResponse::ok).orElseGet(HttpResponse::notFound);
    }

    @Get
    public HttpResponse<List<ChatEntity>> list(@QueryValue(defaultValue = "0") int page,
                                                 @QueryValue(defaultValue = "20") int size) {
        log.info("Received request to list chats page={} size={}", page, size);
        List<ChatEntity> chats = chatManagerService.list(page, size);
        log.info("Returning {} chats", chats.size());
        return HttpResponse.ok(chats);
    }

    @Put("/{id}")
    public HttpResponse<ChatEntity> update(@PathVariable String id, @Body ChatEntity chat) {
        log.info("Received request to update chat id={}: {}", id, chat);
        ChatEntity updated = chatManagerService.update(id, chat);
        log.info("Chat updated: {}", updated);
        return HttpResponse.ok(updated);
    }

    @Delete("/{id}")
    public HttpResponse<Void> delete(@PathVariable String id) {
        log.info("Received request to delete chat by id: {}", id);
        boolean deleted = chatManagerService.deleteById(id);
        if (deleted) {
            log.info("Chat deleted for id: {}", id);
            return HttpResponse.noContent();
        } else {
            log.info("No chat found to delete for id: {}", id);
            return HttpResponse.notFound();
        }
    }
}

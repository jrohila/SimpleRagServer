package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.ChatEntity;
import io.github.jrohila.simpleragserver.repository.ChatManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/chats")
public class ChatManagerController {

    private static final Logger log = LoggerFactory.getLogger(ChatManagerController.class);
    private final ChatManagerService chatManagerService;

    @Autowired
    public ChatManagerController(ChatManagerService chatManagerService) {
        this.chatManagerService = chatManagerService;
    }

    @PostMapping
    public ResponseEntity<ChatEntity> create(@RequestBody ChatEntity chat) {
        log.info("Received request to create chat: {}", chat);
        ChatEntity created = chatManagerService.create(chat);
        log.info("Chat created: {}", created);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatEntity> getById(@PathVariable String id) {
        log.info("Received request to get chat by id: {}", id);
        Optional<ChatEntity> chat = chatManagerService.getById(id);
        if (chat.isPresent()) {
            log.info("Chat found for id: {}", id);
        } else {
            log.info("No chat found for id: {}", id);
        }
        return chat.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ChatEntity>> list(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        log.info("Received request to list chats page={} size={}", page, size);
        List<ChatEntity> chats = chatManagerService.list(page, size);
        log.info("Returning {} chats", chats.size());
        return ResponseEntity.ok(chats);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChatEntity> update(@PathVariable String id, @RequestBody ChatEntity chat) {
        log.info("Received request to update chat id={}: {}", id, chat);
        ChatEntity updated = chatManagerService.update(id, chat);
        log.info("Chat updated: {}", updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("Received request to delete chat by id: {}", id);
        boolean deleted = chatManagerService.deleteById(id);
        if (deleted) {
            log.info("Chat deleted for id: {}", id);
            return ResponseEntity.noContent().build();
        } else {
            log.info("No chat found to delete for id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}

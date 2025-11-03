package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.ChatEntity;
import io.github.jrohila.simpleragserver.repository.ChatManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chats")
public class ChatManagerController {

    private final ChatManagerService chatManagerService;

    @Autowired
    public ChatManagerController(ChatManagerService chatManagerService) {
        this.chatManagerService = chatManagerService;
    }

    @PostMapping
    public ResponseEntity<ChatEntity> create(@RequestBody ChatEntity chat) {
        ChatEntity created = chatManagerService.create(chat);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatEntity> getById(@PathVariable String id) {
        Optional<ChatEntity> chat = chatManagerService.getById(id);
        return chat.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ChatEntity>> list(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        List<ChatEntity> chats = chatManagerService.list(page, size);
        return ResponseEntity.ok(chats);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChatEntity> update(@PathVariable String id, @RequestBody ChatEntity chat) {
        ChatEntity updated = chatManagerService.update(id, chat);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean deleted = chatManagerService.deleteById(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

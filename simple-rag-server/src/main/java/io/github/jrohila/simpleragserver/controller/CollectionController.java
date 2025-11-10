package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.CollectionEntity;
import io.github.jrohila.simpleragserver.repository.CollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    @PostMapping
    public ResponseEntity<CollectionEntity> create(@RequestBody CollectionEntity collection) {
        CollectionEntity created = collectionService.create(collection);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionEntity> getById(@PathVariable String id) {
        Optional<CollectionEntity> collection = collectionService.getById(id);
        return collection.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<CollectionEntity>> list(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        List<CollectionEntity> collections = collectionService.list(page, size);
        return ResponseEntity.ok(collections);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionEntity> update(@PathVariable String id, @RequestBody CollectionEntity collection) {
        CollectionEntity updated = collectionService.update(id, collection);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean deleted = collectionService.deleteById(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

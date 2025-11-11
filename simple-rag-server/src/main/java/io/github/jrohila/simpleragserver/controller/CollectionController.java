package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.CollectionEntity;
import io.github.jrohila.simpleragserver.repository.CollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private static final Logger log = LoggerFactory.getLogger(CollectionController.class);

    @Autowired
    private CollectionService collectionService;

    @PostMapping
    public ResponseEntity<CollectionEntity> create(@RequestBody CollectionEntity collection) {
        log.info("Creating new collection: name={}", collection.getName());
        CollectionEntity created = collectionService.create(collection);
        log.info("Collection created successfully: id={}, name={}", created.getId(), created.getName());
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionEntity> getById(@PathVariable String id) {
        log.debug("Fetching collection by id: {}", id);
        Optional<CollectionEntity> collection = collectionService.getById(id);
        if (collection.isPresent()) {
            log.debug("Collection found: id={}, name={}", id, collection.get().getName());
            return ResponseEntity.ok(collection.get());
        } else {
            log.warn("Collection not found: id={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<CollectionEntity>> list(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        log.debug("Listing collections: page={}, size={}", page, size);
        List<CollectionEntity> collections = collectionService.list(page, size);
        log.info("Retrieved {} collections", collections.size());
        return ResponseEntity.ok(collections);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionEntity> update(@PathVariable String id, @RequestBody CollectionEntity collection) {
        log.info("Updating collection: id={}, name={}", id, collection.getName());
        CollectionEntity updated = collectionService.update(id, collection);
        log.info("Collection updated successfully: id={}, name={}", updated.getId(), updated.getName());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("Deleting collection: id={}", id);
        boolean deleted = collectionService.deleteById(id);
        if (deleted) {
            log.info("Collection deleted successfully: id={}", id);
            return ResponseEntity.noContent().build();
        } else {
            log.warn("Collection not found for deletion: id={}", id);
            return ResponseEntity.notFound().build();
        }
    }
}

package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import io.github.jrohila.simpleragserver.service.ChunkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/chunks")
public class ChunkController {

    private final ChunkService chunkService;

    @Autowired
    public ChunkController(ChunkService chunkService) {
        this.chunkService = chunkService;
    }

    // Create
    @PostMapping
    public ResponseEntity<ChunkEntity> createChunk(@RequestBody ChunkEntity chunk) {
        ChunkEntity saved = chunkService.create(chunk);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Read by id
    @GetMapping("/{id}")
    public ResponseEntity<ChunkEntity> getChunk(@PathVariable String id) {
        Optional<ChunkEntity> chunkOpt = chunkService.getById(id);
        return chunkOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // List (optionally filter by documentId)
    @GetMapping
    public Page<ChunkEntity> listChunks(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "documentId", required = false) String documentId
    ) {
        return chunkService.list(page, size, documentId);
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<ChunkEntity> updateChunk(@PathVariable String id, @RequestBody ChunkEntity chunk) {
        Optional<ChunkEntity> saved = chunkService.update(id, chunk);
        return saved.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Delete by id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChunk(@PathVariable String id) {
        boolean deleted = chunkService.deleteById(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    // Delete all
    @DeleteMapping
    public ResponseEntity<Void> deleteAllChunks() {
        chunkService.deleteAll();
        return ResponseEntity.noContent().build();
    }

    // Optional: delete all by documentId
    @DeleteMapping("/by-document/{documentId}")
    public ResponseEntity<Void> deleteChunksByDocument(@PathVariable String documentId) {
        chunkService.deleteByDocumentId(documentId);
        return ResponseEntity.noContent().build();
    }
}

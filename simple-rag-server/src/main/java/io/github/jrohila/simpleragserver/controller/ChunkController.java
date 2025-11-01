package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.ChunkEntity;
import io.github.jrohila.simpleragserver.service.ChunkService;
import java.util.List;
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
    public ResponseEntity<ChunkEntity> createChunk(@RequestParam(name = "collectionId", required = false) String collectionId, @RequestBody ChunkEntity chunk) {
        ChunkEntity saved = chunkService.create(collectionId, chunk);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Read by id
    @GetMapping("/{collectionId}/{id}")
    public ResponseEntity<ChunkEntity> getChunk(@PathVariable String collectionId, @PathVariable String id) {
        Optional<ChunkEntity> chunkOpt = chunkService.getById(collectionId, id);
        return chunkOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // List (optionally filter by documentId)
    @GetMapping
    public List<ChunkEntity> listChunks(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "collectionId", required = false) String collectionId,
            @RequestParam(name = "documentId", required = false) String documentId
    ) {
        return chunkService.list(collectionId, page, size, documentId);
    }

    // Update
    @PutMapping("/{collectionId}/{id}")
    public ResponseEntity<ChunkEntity> updateChunk(@PathVariable String collectionId, @PathVariable String id, @RequestBody ChunkEntity chunk) {
        Optional<ChunkEntity> saved = chunkService.update(collectionId, id, chunk);
        return saved.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Delete by id
    @DeleteMapping("/{collectionId}/{id}")
    public ResponseEntity<Void> deleteChunk(@PathVariable String collectionId, @PathVariable String id) {
        boolean deleted = chunkService.deleteById(collectionId, id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    // Delete all
    @DeleteMapping("/{collectionId}")
    public ResponseEntity<Void> deleteAllChunks(@PathVariable String collectionId) {
        chunkService.deleteAll(collectionId);
        return ResponseEntity.noContent().build();
    }

    // Optional: delete all by documentId
    @DeleteMapping("/by-document/{collectionId}/{documentId}")
    public ResponseEntity<Void> deleteChunksByDocument(@PathVariable String collectionId, @PathVariable String documentId) {
        chunkService.deleteByDocumentId(collectionId, documentId);
        return ResponseEntity.noContent().build();
    }
}

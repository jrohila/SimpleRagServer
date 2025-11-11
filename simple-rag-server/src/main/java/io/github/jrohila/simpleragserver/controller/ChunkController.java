package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.ChunkEntity;
import io.github.jrohila.simpleragserver.repository.ChunkService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/chunks")
public class ChunkController {

    private static final Logger log = LoggerFactory.getLogger(ChunkController.class);
    private final ChunkService chunkService;

    @Autowired
    public ChunkController(ChunkService chunkService) {
        this.chunkService = chunkService;
    }

    // Create
    @PostMapping
    public ResponseEntity<ChunkEntity> createChunk(@RequestParam(name = "collectionId", required = false) String collectionId, @RequestBody ChunkEntity chunk) {
        log.info("Received request to create chunk in collectionId={}: {}", collectionId, chunk);
        ChunkEntity saved = chunkService.create(collectionId, chunk);
        log.info("Chunk created with id={} in collectionId={}", saved.getId(), collectionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Read by id
    @GetMapping("/{collectionId}/{id}")
    public ResponseEntity<ChunkEntity> getChunk(@PathVariable String collectionId, @PathVariable String id) {
        log.info("Received request to get chunk: collectionId={}, id={}", collectionId, id);
        Optional<ChunkEntity> chunkOpt = chunkService.getById(collectionId, id);
        if (chunkOpt.isPresent()) {
            log.info("Chunk found: collectionId={}, id={}", collectionId, id);
        } else {
            log.info("No chunk found: collectionId={}, id={}", collectionId, id);
        }
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
        log.info("Received request to list chunks: collectionId={}, documentId={}, page={}, size={}", collectionId, documentId, page, size);
        List<ChunkEntity> result = chunkService.list(collectionId, page, size, documentId);
        log.info("Returning {} chunks", result.size());
        return result;
    }

    // Update
    @PutMapping("/{collectionId}/{id}")
    public ResponseEntity<ChunkEntity> updateChunk(@PathVariable String collectionId, @PathVariable String id, @RequestBody ChunkEntity chunk) {
        log.info("Received request to update chunk: collectionId={}, id={}", collectionId, id);
        Optional<ChunkEntity> saved = chunkService.update(collectionId, id, chunk);
        if (saved.isPresent()) {
            log.info("Chunk updated: collectionId={}, id={}", collectionId, id);
        } else {
            log.info("No chunk found to update: collectionId={}, id={}", collectionId, id);
        }
        return saved.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Delete by id
    @DeleteMapping("/{collectionId}/{id}")
    public ResponseEntity<Void> deleteChunk(@PathVariable String collectionId, @PathVariable String id) {
        log.info("Received request to delete chunk: collectionId={}, id={}", collectionId, id);
        boolean deleted = chunkService.deleteById(collectionId, id);
        if (!deleted) {
            log.info("No chunk found to delete: collectionId={}, id={}", collectionId, id);
            return ResponseEntity.notFound().build();
        }
        log.info("Chunk deleted: collectionId={}, id={}", collectionId, id);
        return ResponseEntity.noContent().build();
    }

    // Delete all
    @DeleteMapping("/{collectionId}")
    public ResponseEntity<Void> deleteAllChunks(@PathVariable String collectionId) {
        log.info("Received request to delete all chunks in collectionId={}", collectionId);
        chunkService.deleteAll(collectionId);
        log.info("All chunks deleted in collectionId={}", collectionId);
        return ResponseEntity.noContent().build();
    }

    // Optional: delete all by documentId
    @DeleteMapping("/by-document/{collectionId}/{documentId}")
    public ResponseEntity<Void> deleteChunksByDocument(@PathVariable String collectionId, @PathVariable String documentId) {
        log.info("Received request to delete all chunks by documentId={} in collectionId={}", documentId, collectionId);
        chunkService.deleteByDocumentId(collectionId, documentId);
        log.info("All chunks deleted by documentId={} in collectionId={}", documentId, collectionId);
        return ResponseEntity.noContent().build();
    }
}

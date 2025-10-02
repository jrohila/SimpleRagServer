package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import io.github.jrohila.simpleragserver.repository.ChunkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ChunkService {

    private final ChunkRepository chunkRepository;

    @Autowired
    public ChunkService(ChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    public ChunkEntity create(ChunkEntity chunk) {
        // Require hash and ensure uniqueness
        if (chunk.getHash() == null || chunk.getHash().isBlank()) {
            throw new IllegalArgumentException("Chunk hash is required");
        }
        if (chunkRepository.existsByHash(chunk.getHash())) {
            throw new IllegalStateException("Chunk with the same hash already exists");
        }
        return chunkRepository.save(chunk);
    }

    public Optional<ChunkEntity> getById(String id) {
        return chunkRepository.findById(id);
    }

    public Page<ChunkEntity> list(int page, int size, String documentId) {
        Pageable pageable = PageRequest.of(page, size);
        if (documentId != null && !documentId.isEmpty()) {
            return chunkRepository.findByDocumentId(documentId, pageable);
        }
        return chunkRepository.findAll(pageable);
    }

    public Optional<ChunkEntity> update(String id, ChunkEntity chunk) {
        if (!chunkRepository.existsById(id)) {
            return Optional.empty();
        }
        if (chunk.getHash() == null || chunk.getHash().isBlank()) {
            throw new IllegalArgumentException("Chunk hash is required");
        }
        var existingWithHash = chunkRepository.findFirstByHash(chunk.getHash());
        if (existingWithHash.isPresent() && !existingWithHash.get().getId().equals(id)) {
            throw new IllegalStateException("Another chunk with the same hash exists");
        }
        chunk.setId(id);
        return Optional.of(chunkRepository.save(chunk));
    }

    public boolean deleteById(String id) {
        if (!chunkRepository.existsById(id)) {
            return false;
        }
        chunkRepository.deleteById(id);
        return true;
    }

    public void deleteAll() {
        chunkRepository.deleteAll();
    }

    public void deleteByDocumentId(String documentId) {
        chunkRepository.deleteByDocumentId(documentId);
    }
}
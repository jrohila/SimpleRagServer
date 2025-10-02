package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ChunkRepository extends ElasticsearchRepository<ChunkEntity, String> {

    Page<ChunkEntity> findByDocumentId(String documentId, Pageable pageable);

    void deleteByDocumentId(String documentId);

    // New helpers for hash uniqueness
    boolean existsByHash(String hash);

    Optional<ChunkEntity> findFirstByHash(String hash);
}

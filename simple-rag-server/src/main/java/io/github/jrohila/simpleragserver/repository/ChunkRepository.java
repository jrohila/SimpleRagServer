package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChunkRepository extends ElasticsearchRepository<ChunkEntity, String> {

    Page<ChunkEntity> findByDocumentId(String documentId, Pageable pageable);

    void deleteByDocumentId(String documentId);
}

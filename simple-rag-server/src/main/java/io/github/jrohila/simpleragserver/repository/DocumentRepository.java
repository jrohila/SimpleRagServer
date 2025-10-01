package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DocumentRepository extends ElasticsearchRepository<DocumentEntity, String> {

    Page<DocumentEntity> findByOriginalFilenameContaining(String filenamePart, Pageable pageable);

    Page<DocumentEntity> findByHash(String hash, Pageable pageable);
}
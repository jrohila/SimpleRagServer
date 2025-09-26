package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.model.DocumentDto;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Repository
public class DocumentDAO {
    private final VectorStore vectorStore;
    private static final Logger LOGGER = Logger.getLogger(DocumentDAO.class.getName());

    @Autowired
    public DocumentDAO(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public DocumentDto create(DocumentDto doc) {
        if (doc.getId() == null || doc.getId().isBlank()) {
            doc.setId(UUID.randomUUID().toString());
        }
        long start = System.currentTimeMillis();
        Document vectorDoc = DocumentMapper.toDocument(doc);
        try {
            vectorStore.add(List.of(vectorDoc));
            long ms = System.currentTimeMillis() - start;
            LOGGER.fine(() -> "DocumentDAO.create: id=" + doc.getId() + " durationMs=" + ms);
        } catch (RuntimeException ex) {
            long ms = System.currentTimeMillis() - start;
            LOGGER.log(Level.SEVERE, "DocumentDAO.create: failure id=" + doc.getId() + " durationMs=" + ms, ex);
            throw ex;
        }
        return doc;
    }

    public Optional<DocumentDto> findById(String id) {
        long start = System.currentTimeMillis();
        SearchRequest request = SearchRequest.builder()
            .query(id != null && !id.isBlank() ? id : "document")
            .topK(1)
            .filterExpression("id == '" + id + "'")
            .build();
        List<Document> docs = vectorStore.similaritySearch(request);
        long ms = System.currentTimeMillis() - start;
        LOGGER.fine(() -> "DocumentDAO.findById: id=" + id + " hits=" + docs.size() + " durationMs=" + ms);
        if (docs.isEmpty()) return Optional.empty();
        return Optional.of(DocumentMapper.fromDocument(docs.get(0)));
    }

    public List<DocumentDto> findAll() {
        long start = System.currentTimeMillis();
        SearchRequest request = SearchRequest.builder()
            .query("document")
            .topK(10000)
            .build();
        List<Document> docs = vectorStore.similaritySearch(request);
        long ms = System.currentTimeMillis() - start;
        LOGGER.fine(() -> "DocumentDAO.findAll: returned=" + docs.size() + " durationMs=" + ms);
        List<DocumentDto> result = new ArrayList<>();
        for (Document d : docs) result.add(DocumentMapper.fromDocument(d));
        return result;
    }

    public DocumentDto save(DocumentDto doc) {
        if (doc.getId() == null || doc.getId().isBlank()) {
            throw new IllegalArgumentException("Document id must not be null for save");
        }
        long start = System.currentTimeMillis();
        try {
            vectorStore.add(List.of(DocumentMapper.toDocument(doc)));
            long ms = System.currentTimeMillis() - start;
            LOGGER.fine(() -> "DocumentDAO.save: id=" + doc.getId() + " durationMs=" + ms);
        } catch (RuntimeException ex) {
            long ms = System.currentTimeMillis() - start;
            LOGGER.log(Level.SEVERE, "DocumentDAO.save: failure id=" + doc.getId() + " durationMs=" + ms, ex);
            throw ex;
        }
        return doc;
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    public void deleteById(String id) {
        long start = System.currentTimeMillis();
        SearchRequest request = SearchRequest.builder()
            .query(id != null && !id.isBlank() ? id : "document")
            .topK(10000)
            .filterExpression("id == '" + id + "'")
            .build();
        List<Document> docs = vectorStore.similaritySearch(request);
        if (!docs.isEmpty()) {
            vectorStore.delete(docs.stream().map(Document::getId).toList());
        }
        long ms = System.currentTimeMillis() - start;
        LOGGER.fine(() -> "DocumentDAO.deleteById: id=" + id + " removed=" + (docs != null ? docs.size() : 0) + " durationMs=" + ms);
    }
}

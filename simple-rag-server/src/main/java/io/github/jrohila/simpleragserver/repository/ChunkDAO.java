package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.model.ChunkDto;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

@Repository
public class ChunkDAO {
    private final VectorStore vectorStore;

    @Autowired
    public ChunkDAO(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // Create a new ChunkDto, vector is calculated from text
    public ChunkDto create(ChunkDto chunk) {
        if (chunk.getId() == null || chunk.getId().isBlank()) {
            chunk.setId(UUID.randomUUID().toString());
        }
    Document doc = ChunkMapper.toDocument(chunk);
    vectorStore.add(List.of(doc));
        return chunk;
    }


    // Update an existing ChunkDto
    public ChunkDto update(ChunkDto chunk) {
        if (chunk.getId() == null || chunk.getId().isBlank()) {
            throw new IllegalArgumentException("ChunkDto id must be set for update");
        }
    Document doc = ChunkMapper.toDocument(chunk);
    vectorStore.add(List.of(doc));
        return chunk;
    }


    // Read a ChunkDto by id (if supported by your VectorStore implementation)
    public ChunkDto read(String id) {
        SearchRequest request = SearchRequest.builder()
            .query(id != null && !id.isBlank() ? id : "chunk")
            .topK(1)                  // just need the match
            .filterExpression("id == '" + id + "'")
            .build();

        List<Document> results = vectorStore.similaritySearch(request);
        if (results.isEmpty()) {
            return null;
        }
        return ChunkMapper.fromDocument(results.get(0));
    }

    // Vector search for chunks by prompt
    public List<ChunkDto> vectorSearch(String prompt, int maxResults) {
        String q = (prompt != null && !prompt.isBlank()) ? prompt : "chunk";
        SearchRequest request = SearchRequest.builder()
            .query(q)
            .topK(maxResults)
            .filterExpression("usage == 'RAG'")
            // ensure we only retrieve chunks that have an associated documentId
            .build();
                
        List<Document> docs = vectorStore.similaritySearch(request);
        List<ChunkDto> results = new ArrayList<>();
        for (Document doc : docs) {
            results.add(ChunkMapper.fromDocument(doc));
        }
        return results;
    }

    // Delete all chunks by documentId
    public void deleteAllByDocumentId(String documentId) {
        SearchRequest request = SearchRequest.builder()
            .query(documentId != null && !documentId.isBlank() ? documentId : "chunk")
            .topK(10000)
            .filterExpression("documentId == '" + documentId + "'")
            .build();
        List<Document> docs = vectorStore.similaritySearch(request);
        if (docs != null && !docs.isEmpty()) {
            vectorStore.delete(docs.stream().map(Document::getId).toList());
        }
    }

}

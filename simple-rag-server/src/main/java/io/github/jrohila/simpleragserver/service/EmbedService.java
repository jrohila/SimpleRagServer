package io.github.jrohila.simpleragserver.service;

import java.util.Collections;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.embedding.EmbeddingModel;

@Service
public class EmbedService {

    private final EmbeddingModel embeddingModel;

    @Autowired
    public EmbedService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Gets the embedding vector for the given text from Ollama using Spring AI 1.0.
     * @param text the input text
     * @return embedding vector as double[]
     */
    /**
     * Gets the embedding vector for the given text from Ollama using Spring AI 1.0.
     *
     * @param text the input text
     * @return embedding vector as double[]
     */
    public float[] getEmbedding(String text) {
        // Wrap the text into a request
        EmbeddingRequest request = new EmbeddingRequest(Collections.singletonList(text), null);

        // Call the embedding model
        EmbeddingResponse response = embeddingModel.call(request);
        return response.getResult().getOutput();
    }

    /**
     * Gets the embedding vector for the given text as a List<Float>.
     *
     * @param text the input text
     * @return embedding vector as List<Float>
     */
    public List<Float> getEmbeddingAsList(String text) {
        float[] embeddingArray = getEmbedding(text);
        List<Float> embeddingList = new java.util.ArrayList<>(embeddingArray.length);
        for (float v : embeddingArray) {
            embeddingList.add(v);
        }
        return embeddingList;
    }
}

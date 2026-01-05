package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.client.EmbeddingClientFactory;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

@Controller("/api/client")
public class ClientController {
    
    private final EmbeddingClientFactory embeddingClientFactory;

    public ClientController(EmbeddingClientFactory embeddingClientFactory) {
        this.embeddingClientFactory = embeddingClientFactory;
    }

    @Post("/embed")
    public HttpResponse<float[]> calculateEmbedding(@Body String input) {        
        float[] embedding = embeddingClientFactory.getDefaultClient().embed(input);
        // Convert double[] to float[]
        float[] floatEmbedding = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            floatEmbedding[i] = (float) embedding[i];
        }
        return HttpResponse.ok(floatEmbedding);
    }
}

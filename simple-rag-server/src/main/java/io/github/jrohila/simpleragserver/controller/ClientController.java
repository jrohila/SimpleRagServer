package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.client.EmbedClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/client")
public class ClientController {
    @Autowired
    private EmbedClient embedService;

    @PostMapping("/embed")
    public ResponseEntity<float[]> calculateEmbedding(@RequestBody String input) {
        float[] embedding = embedService.getEmbedding(input);
        // Convert double[] to float[]
        float[] floatEmbedding = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            floatEmbedding[i] = (float) embedding[i];
        }
        return ResponseEntity.ok(floatEmbedding);
    }
}

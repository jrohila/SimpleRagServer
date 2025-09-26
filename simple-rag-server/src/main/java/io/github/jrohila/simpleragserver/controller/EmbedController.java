package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.service.EmbedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/embed")
public class EmbedController {
    @Autowired
    private EmbedService embedService;

    @PostMapping("/calculateEmbedding")
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

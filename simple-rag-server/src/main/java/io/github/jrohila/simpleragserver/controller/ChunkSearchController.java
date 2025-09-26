package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.model.ChunkDto;
import io.github.jrohila.simpleragserver.repository.ChunkDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chunks")
public class ChunkSearchController {
    @Autowired
    private ChunkDAO chunkDAO;

    @GetMapping("/search")
    public ResponseEntity<List<ChunkDto>> searchChunks(
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "maxResults", defaultValue = "5") int maxResults) {
        List<ChunkDto> results = chunkDAO.vectorSearch(prompt, maxResults);
        return ResponseEntity.ok(results);
    }
}

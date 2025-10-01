package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.service.HybridSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final HybridSearchService hybridSearchService;

    @Autowired
    public SearchController(HybridSearchService hybridSearchService) {
        this.hybridSearchService = hybridSearchService;
    }

    @GetMapping
    public Map<String, Object> searchChunks(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "RAG") String type,
            @RequestParam(required = false, defaultValue = "simple_rag_server") String index
    ) throws Exception {
        return hybridSearchService.hybridSearch(query, type, index);
    }
}

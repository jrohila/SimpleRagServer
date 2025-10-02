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
            @RequestParam String query
    ) throws Exception {
        return hybridSearchService.hybridSearch(query);
    }
}

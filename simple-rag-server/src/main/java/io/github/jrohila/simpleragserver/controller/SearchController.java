package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.service.SearchService;
import io.github.jrohila.simpleragserver.dto.SearchResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService hybridSearchService) {
        this.searchService = hybridSearchService;
    }

    // Raw response
    @GetMapping("/raw")
    public Map<String, Object> searchChunks(
            @RequestParam String query,
        @RequestParam(name = "matchType", defaultValue = "MATCH") SearchService.MatchType matchType,
            @RequestParam(name = "useKnn", defaultValue = "true") boolean useKnn,
            @RequestParam(name = "size", defaultValue = "25") int size
    ) throws Exception {
        return searchService.searchAsRaw(query, matchType, useKnn, size);
    }

    // DTO response
    @GetMapping
    public List<SearchResultDTO> searchChunksAsDto(
            @RequestParam String query,
            @RequestParam(name = "matchType", defaultValue = "MATCH") SearchService.MatchType matchType,
            @RequestParam(name = "useKnn", defaultValue = "true") boolean useKnn,
            @RequestParam(name = "size", defaultValue = "25") int size
    ) throws Exception {
        return searchService.search(query, matchType, useKnn, size);
    }
}

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

    private final SearchService hybridSearchService;

    @Autowired
    public SearchController(SearchService hybridSearchService) {
        this.hybridSearchService = hybridSearchService;
    }

    // Raw response
    @GetMapping("/raw")
    public Map<String, Object> searchChunks(
            @RequestParam String query,
            @RequestParam(name = "matchType", defaultValue = "Match") SearchService.MatchType matchType,
            @RequestParam(name = "useKnn", defaultValue = "true") boolean useKnn
    ) throws Exception {
        return hybridSearchService.hybridSearch(query, matchType, useKnn);
    }

    // DTO response
    @GetMapping
    public List<SearchResultDTO> searchChunksAsDto(
            @RequestParam String query,
            @RequestParam(name = "matchType", defaultValue = "Match") SearchService.MatchType matchType,
            @RequestParam(name = "useKnn", defaultValue = "true") boolean useKnn
    ) throws Exception {
        return hybridSearchService.hybridSearchAsDto(query, matchType, useKnn);
    }
}

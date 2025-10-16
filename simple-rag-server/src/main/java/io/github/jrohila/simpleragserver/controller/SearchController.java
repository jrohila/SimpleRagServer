package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.service.SearchService;
import io.github.jrohila.simpleragserver.dto.SearchResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import io.github.jrohila.simpleragserver.service.SummarizerService;

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

    // Summary response (plain string)
    @GetMapping("/summary")
    public String searchSummary(
            @RequestParam String query,
            @RequestParam(name = "matchType", defaultValue = "MATCH") SearchService.MatchType matchType,
            @RequestParam(name = "useKnn", defaultValue = "true") boolean useKnn,
            @RequestParam(name = "size", defaultValue = "25") int size,
            @RequestParam(name = "maxWords", defaultValue = "200") int maxWords,
            @RequestParam(name = "method", required = false, defaultValue = "META") SummarizerService.Method method
    ) throws Exception {
        return searchService.searchAndSummarize(query, matchType, useKnn, size, maxWords, method);
    }

    // Clustered response (nested lists of DTOs)
    @GetMapping("/cluster")
    public List<List<SearchResultDTO>> searchClusters(
            @RequestParam String query,
            @RequestParam(name = "matchType", defaultValue = "MATCH") SearchService.MatchType matchType,
            @RequestParam(name = "useKnn", defaultValue = "true") boolean useKnn,
            @RequestParam(name = "size", defaultValue = "25") int size,
            @RequestParam(name = "minPts", required = false) Integer minPts,
            @RequestParam(name = "eps", required = false) Double eps
    ) throws Exception {
        return searchService.searchAndCluster(query, matchType, useKnn, size, minPts, eps);
    }
}

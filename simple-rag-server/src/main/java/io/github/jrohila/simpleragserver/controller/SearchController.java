package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.controller.util.HybridSearchRequest;
import io.github.jrohila.simpleragserver.controller.util.SearchResultDtoMapper;
import io.github.jrohila.simpleragserver.controller.util.Term;
import io.github.jrohila.simpleragserver.controller.util.VectorSearchRequest;
import io.github.jrohila.simpleragserver.domain.SearchResultDTO;
import io.micronaut.http.annotation.*;
import io.micronaut.http.MediaType;

import java.util.List;
import io.github.jrohila.simpleragserver.domain.ChunkEntity;
import io.github.jrohila.simpleragserver.repository.ChunkSearchService;
import io.github.jrohila.simpleragserver.service.util.SearchResult;
import java.util.ArrayList;
import io.github.jrohila.simpleragserver.service.util.SearchTerm;

@Controller("/api/search")
public class SearchController {

    private final ChunkSearchService chunkSearchService;

    public SearchController(ChunkSearchService chunkSearchService) {
        this.chunkSearchService = chunkSearchService;
    }

    // Lexical-only search (no vector, no hybrid), same payload as hybrid
    @Post(uri = "/lexical", consumes = MediaType.APPLICATION_JSON)
    public List<SearchResultDTO> lexicalSearch(@QueryValue String collectionId, @Body HybridSearchRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        String query = req.getQuery();
        ChunkSearchService.MatchType matchType = (req.getMatchType() == null)
                ? ChunkSearchService.MatchType.MATCH
                : req.getMatchType();
        int size = (req.getSize() == null || req.getSize() <= 0) ? 25 : req.getSize();
        boolean enableFuzziness = Boolean.TRUE.equals(req.getEnableFuzziness());
        String language = req.getLanguage();

        // Convert incoming terms to service terms
        List<SearchTerm> svcTerms = new ArrayList<>();
        if (req.getTerms() != null) {
            for (Term t : req.getTerms()) {
                if (t == null || t.getTerm() == null || t.getTerm().isBlank()) {
                    continue;
                }
                SearchTerm st = new SearchTerm();
                st.setTerm(t.getTerm());
                st.setBoostWeight(t.getBoostWeight());
                st.setMandatory(Boolean.TRUE.equals(t.getMandatory()));
                svcTerms.add(st);
            }
        }

        List<SearchResult<ChunkEntity>> hits = chunkSearchService.lexicalSearch(collectionId, query, matchType, svcTerms, size, enableFuzziness, language);

        // Map to DTOs
        List<SearchResultDTO> out = new ArrayList<>();
        hits.forEach(hit -> {
            out.add(SearchResultDtoMapper.mapChunkEntity(hit.getContent(), (float) hit.getScore()));
        });
        return out;
    }

    // Pure vector search (kNN) with optional language and mandatory term filters, plus client-side rerank by boost weights
    @Post(uri = "/vector", consumes = MediaType.APPLICATION_JSON)
    public List<SearchResultDTO> vectorSearch(@QueryValue String collectionId, @Body VectorSearchRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        String query = req.getQuery();
        int size = (req.getSize() == null || req.getSize() <= 0) ? 25 : req.getSize();
        String language = req.getLanguage();

        List<SearchTerm> svcTerms = new ArrayList<>();
        if (req.getTerms() != null) {
            for (Term t : req.getTerms()) {
                if (t == null || t.getTerm() == null || t.getTerm().isBlank()) {
                    continue;
                }
                SearchTerm st = new SearchTerm();
                st.setTerm(t.getTerm());
                st.setBoostWeight(t.getBoostWeight());
                st.setMandatory(Boolean.TRUE.equals(t.getMandatory()));
                svcTerms.add(st);
            }
        }

        List<SearchResult<ChunkEntity>> hits = chunkSearchService.vectorSearch(collectionId, query, svcTerms, size, language);

        List<SearchResultDTO> out = new ArrayList<>();
        hits.forEach(hit -> {
            out.add(SearchResultDtoMapper.mapChunkEntity(hit.getContent(), (float) hit.getScore()));
        });
        return out;
    }

    // Hybrid search using Spring Data OpenSearch (lexical + kNN with per-term boost and mandatory filters)
    @Post(uri = "/hybrid", consumes = MediaType.APPLICATION_JSON)
    public List<SearchResultDTO> hybridSearch(@QueryValue String collectionId, @Body HybridSearchRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        String query = req.getQuery();
        ChunkSearchService.MatchType matchType = (req.getMatchType() == null)
                ? ChunkSearchService.MatchType.MATCH
                : req.getMatchType();
        int size = (req.getSize() == null || req.getSize() <= 0) ? 25 : req.getSize();
        boolean enableFuzziness = Boolean.TRUE.equals(req.getEnableFuzziness());
        String language = req.getLanguage();

        // Convert incoming terms to service terms (inner class)
        List<SearchTerm> svcTerms = new ArrayList<>();
        if (req.getTerms() != null) {
            for (Term t : req.getTerms()) {
                if (t == null || t.getTerm() == null || t.getTerm().isBlank()) {
                    continue;
                }
                SearchTerm st = new SearchTerm();
                st.setTerm(t.getTerm());
                st.setBoostWeight(t.getBoostWeight());
                st.setMandatory(Boolean.TRUE.equals(t.getMandatory()));
                svcTerms.add(st);
            }
        }

        List<SearchResult<ChunkEntity>> hits = chunkSearchService.hybridSearch(collectionId, query, matchType, svcTerms, size, enableFuzziness, language);
        // Map to SearchResultDTO for consistent response shape
        List<SearchResultDTO> out = new ArrayList<>();
        hits.forEach(hit -> {
            out.add(SearchResultDtoMapper.mapChunkEntity(hit.getContent(), (float) hit.getScore()));
        });
        return out;
    }

}

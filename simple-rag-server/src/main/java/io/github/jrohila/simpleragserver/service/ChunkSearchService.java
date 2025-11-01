/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.client.EmbedClient;
import io.github.jrohila.simpleragserver.domain.ChunkEntity;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.github.jrohila.simpleragserver.service.util.SearchResult;
import io.github.jrohila.simpleragserver.service.util.SearchTerm;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.List;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
// Note: NativeSearchQueryBuilder isn't available in this project's dependencies; we'll use StringQuery.

/**
 *
 * @author Jukka
 */
@Service
public class ChunkSearchService {

    @Autowired
    private OpenSearchClient openSearchClient;

    private static final Logger log = LoggerFactory.getLogger(ChunkSearchService.class);

    @Autowired
    private EmbedClient embedClient;

    @Autowired
    private SummarizerService summarizerService;

    @Autowired
    private IndicesManager indicesManager;
    
    // Limit the number of boosted terms added to the query
    private static final int MAX_TERMS = 12;

    public enum MatchType {
        MATCH, MATCH_PHRASE, MATCH_BOOL_PREFIX, QUERY_STRING, SIMPLE_QUERY_STRING
    };

    public List<SearchResult<ChunkEntity>> vectorSearch(String collectionId, String query, List<SearchTerm> terms, int size, String language) {
        try {
            // Build embedding for kNN
            List<Float> embedding = embedClient.getEmbeddingAsList(query);
            int k = Math.max(1, size);

            // Build filter queries from language and mandatory terms
            List<Query> filterQueries = new ArrayList<>();
            if (language != null && !language.isBlank()) {
                filterQueries.add(Query.of(q -> q.term(t -> t.field("language").value(org.opensearch.client.opensearch._types.FieldValue.of(language)))));
            }
            if (terms != null && !terms.isEmpty()) {
                Set<String> seen = new LinkedHashSet<>();
                int applied = 0;
                for (SearchTerm t : terms) {
                    if (t == null || t.getTerm() == null || t.getTerm().isBlank()) {
                        continue;
                    }
                    if (!t.isMandatory()) {
                        continue; // only mandatory terms used for filtering
                    }
                    String raw = t.getTerm().trim();
                    String key = raw.toLowerCase(Locale.ROOT);
                    if (!seen.add(key)) {
                        continue; // de-duplicate
                    }
                    if (applied >= MAX_TERMS) {
                        continue; // cap
                    }
                    filterQueries.add(Query.of(q -> q.matchPhrase(mp -> mp.field("text").query(raw))));
                    applied++;
                }
                if (seen.size() > MAX_TERMS) {
                    try {
                        log.info("vectorSearch: filter terms truncated to {} (from {}) due to MAX_TERMS cap", MAX_TERMS, seen.size());
                    } catch (Exception ignore) {
                    }
                }
            }

            // Build kNN query
            Query knnQuery = Query.of(q -> q.knn(kq -> kq.field("embedding").vector(embedding).k(k)));

            // Compose bool query: must = knn, filter = filterQueries
            Query boolQuery = Query.of(q -> q.bool(b -> b
                    .must(knnQuery)
                    .filter(filterQueries)
            ));

            String indexName = this.indicesManager.createIfNotExist(collectionId, ChunkEntity.class);
            
            SearchRequest searchRequest = SearchRequest.of(b -> b
                    .index(indexName)
                    .size(size)
                    .query(boolQuery)
            );

            log.info("Vector kNN query: {}", searchRequest.toString());

            SearchResponse<ChunkEntity> resp = openSearchClient.search(searchRequest, ChunkEntity.class);
            return this.processSearchResponse(resp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute vector search", e);
        }
    }

    public String summarySearch(String collectionId, String query, MatchType matchType, List<SearchTerm> terms, int size, boolean enableFuzziness, String language) {
        List<SearchResult<ChunkEntity>> results = this.hybridSearch(collectionId, query, matchType, terms, size, enableFuzziness, language);
        StringBuilder combined = new StringBuilder();
        for (SearchResult<ChunkEntity> result : results) {
            combined.append(this.summarizerService.summarize(result.getContent().getText(), -1, SummarizerService.Method.BART));
            combined.append(System.lineSeparator());
        }
        return combined.toString();
    }

    // Note: client-side rescoring removed for simplicity; results are returned as-is from kNN.
    // (Optional cap can be added later if needed)
    /**
     * Hybrid search combining lexical and kNN.
     *
     * Notes: - enableFuzziness applies only to MATCH queries on the "text"
     * field; others are unchanged.
     */
    public List<SearchResult<ChunkEntity>> hybridSearch(String collectionId, String query, MatchType matchType, List<SearchTerm> terms, int size, boolean enableFuzziness, String language) {
        try {
            // Build embedding for kNN
            List<Float> embedding = embedClient.getEmbeddingAsList(query);
            int k = Math.max(1, size);

            // Build lexical clause as an OpenSearch Query using the selected matchType
            Query matchQuery;
            switch (matchType) {
                case MATCH -> {
                    if (enableFuzziness) {
                        matchQuery = Query.of(q -> q.match(m -> m.field("text").query(FieldValue.of(query)).fuzziness("AUTO").prefixLength(1).maxExpansions(50)));
                    } else {
                        matchQuery = Query.of(q -> q.match(m -> m.field("text").query(FieldValue.of(query))));
                    }
                }
                case MATCH_PHRASE ->
                    matchQuery = Query.of(q -> q.matchPhrase(mp -> mp.field("text").query(query)));
                case MATCH_BOOL_PREFIX ->
                    matchQuery = Query.of(q -> q.matchBoolPrefix(mbp -> mbp.field("text").query(query)));
                case QUERY_STRING ->
                    matchQuery = Query.of(q -> q.queryString(qs -> qs.query(query)));
                case SIMPLE_QUERY_STRING ->
                    matchQuery = Query.of(q -> q.simpleQueryString(sqs -> sqs.query(query)));
                default ->
                    matchQuery = Query.of(q -> q.match(m -> m.field("text").query(FieldValue.of(query))));
            }

            // Boosting and mandatory filters
            List<Query> shouldBoostQueries = new ArrayList<>();
            List<Query> mandatoryFilterQueries = new ArrayList<>();
            if (language != null && !language.isBlank()) {
                mandatoryFilterQueries.add(Query.of(q -> q.term(t -> t.field("language").value(FieldValue.of(language)))));
            }
            if (terms != null) {
                Set<String> seen = new LinkedHashSet<>();
                int applied = 0;
                for (SearchTerm t : terms) {
                    if (t == null) {
                        continue;
                    }
                    String raw = t.getTerm();
                    if (raw == null || raw.isBlank()) {
                        continue;
                    }
                    String term = raw.trim();
                    String key = term.toLowerCase(Locale.ROOT);
                    if (!seen.add(key)) {
                        continue;
                    }
                    if (applied >= MAX_TERMS) {
                        continue;
                    }
                    Double boost = t.getBoostWeight();
                    if (boost != null) {
                        shouldBoostQueries.add(Query.of(q -> q.matchPhrase(mp -> mp.field("text").query(term).boost(boost.floatValue()))));
                    } else {
                        shouldBoostQueries.add(Query.of(q -> q.matchPhrase(mp -> mp.field("text").query(term))));
                    }
                    if (t.isMandatory()) {
                        mandatoryFilterQueries.add(Query.of(q -> q.matchPhrase(mp -> mp.field("text").query(term))));
                    }
                    applied++;
                }
                if (seen.size() > MAX_TERMS) {
                    try {
                        log.info("hybridSearch: terms truncated to {} (from {}) due to MAX_TERMS cap", MAX_TERMS, seen.size());
                    } catch (Exception ignore) {
                    }
                }
            }

            // Build knn query
            Query knnQuery = Query.of(q -> q.knn(kq -> kq.field("embedding").vector(embedding).k(k)));

            // Compose should clause: matchQuery, knnQuery, shouldBoostQueries
            List<Query> shouldQueries = new ArrayList<>();
            shouldQueries.add(matchQuery);
            shouldQueries.add(knnQuery);
            shouldQueries.addAll(shouldBoostQueries);

            // Compose filter clause: mandatoryFilterQueries
            List<Query> filterQueries = new ArrayList<>(mandatoryFilterQueries);

            String indexName = this.indicesManager.createIfNotExist(collectionId, ChunkEntity.class);
            
            SearchRequest searchRequest = SearchRequest.of(b -> b
                    .index(indexName)
                    .pipeline("rrf-pipeline")
                    .size(size)
                    .query(q -> q.bool(bb -> bb
                    .should(shouldQueries)
                    .filter(filterQueries)
                    .minimumShouldMatch("1")
            ))
            );

            // Execute using OpenSearch client
            SearchResponse<ChunkEntity> resp = openSearchClient.search(searchRequest, ChunkEntity.class);
            long totalHits = 0;
            var hits = resp.hits();
            if (hits != null && hits.total() != null) {
                totalHits = hits.total().value();
            }
            log.info("OpenSearch hybrid response: total hits = {}", totalHits);

            return this.processSearchResponse(resp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute hybrid search (OpenSearch client)", e);
        }
    }

    public List<SearchResult<ChunkEntity>> lexicalSearch(String collectionId, String query, MatchType matchType, List<SearchTerm> terms, int size, boolean enableFuzziness, String language) {
        try {
            // 1) Build base lexical clause as Query
            Query baseTextQuery;
            switch (matchType) {
                case MATCH -> {
                    if (enableFuzziness) {
                        baseTextQuery = Query.of(q -> q.match(m -> m
                                .field("text")
                                .query(org.opensearch.client.opensearch._types.FieldValue.of(query))
                                .fuzziness("AUTO")
                                .prefixLength(1)
                                .maxExpansions(50)
                        ));
                    } else {
                        baseTextQuery = Query.of(q -> q.match(m -> m.field("text").query(org.opensearch.client.opensearch._types.FieldValue.of(query))));
                    }
                }
                case MATCH_PHRASE ->
                    baseTextQuery = Query.of(q -> q.matchPhrase(mp -> mp.field("text").query(query)));
                case MATCH_BOOL_PREFIX ->
                    baseTextQuery = Query.of(q -> q.matchBoolPrefix(mbp -> mbp.field("text").query(query)));
                case QUERY_STRING ->
                    baseTextQuery = Query.of(q -> q.queryString(qs -> qs.query(query)));
                case SIMPLE_QUERY_STRING ->
                    baseTextQuery = Query.of(q -> q.simpleQueryString(sqs -> sqs.query(query)));
                default ->
                    baseTextQuery = Query.of(q -> q.match(m -> m.field("text").query(org.opensearch.client.opensearch._types.FieldValue.of(query))));
            }

            // 2) Collect boosts and filters (language + mandatory terms)
            List<Query> shouldBoost = new ArrayList<>();
            List<Query> filters = new ArrayList<>();

            if (language != null && !language.isBlank()) {
                filters.add(Query.of(q -> q.term(t -> t.field("language").value(org.opensearch.client.opensearch._types.FieldValue.of(language)))));
            }

            if (terms != null && !terms.isEmpty()) {
                Set<String> seen = new LinkedHashSet<>();
                int applied = 0;
                for (SearchTerm t : terms) {
                    if (t == null) {
                        continue;
                    }
                    String raw = t.getTerm();
                    if (raw == null || raw.isBlank()) {
                        continue;
                    }
                    String term = raw.trim();
                    String key = term.toLowerCase(Locale.ROOT);
                    if (!seen.add(key)) {
                        continue;       // dedupe
                    }
                    if (applied >= MAX_TERMS) {
                        continue;  // cap
                    }
                    // Boosting clause (match_phrase with optional boost)
                    if (t.getBoostWeight() != null) {
                        shouldBoost.add(Query.of(q -> q.matchPhrase(mp -> mp.field("text").query(term).boost(t.getBoostWeight().floatValue()))));
                    } else {
                        shouldBoost.add(Query.of(q -> q.matchPhrase(mp -> mp.field("text").query(term))));
                    }

                    // Mandatory filter
                    if (t.isMandatory()) {
                        filters.add(Query.of(q -> q.matchPhrase(mp -> mp.field("text").query(term))));
                    }
                    applied++;
                }
                if (seen.size() > MAX_TERMS) {
                    try {
                        log.info("lexicalSearch: terms truncated to {} (from {}) due to MAX_TERMS cap", MAX_TERMS, seen.size());
                    } catch (Exception ignore) {
                    }
                }
            }

            // 3) Build bool query: must baseTextQuery, should boosts, filter mandatory terms/lang
            Query boolQuery = Query.of(q -> q.bool(b -> b
                    .must(baseTextQuery)
                    .should(shouldBoost)
                    .filter(filters)
            ));

            String indexName = this.indicesManager.createIfNotExist(collectionId, ChunkEntity.class);
            
            SearchRequest searchRequest = SearchRequest.of(b -> b
                    .index(indexName)
                    .size(size)
                    .query(boolQuery)
            );

            log.info("Lexical query: {}", searchRequest.toString());

            SearchResponse<ChunkEntity> resp = openSearchClient.search(searchRequest, ChunkEntity.class);
            return this.processSearchResponse(resp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute lexical search", e);
        }
    }

    private List<SearchResult<ChunkEntity>> processSearchResponse(SearchResponse<ChunkEntity> response) {
        List<SearchResult<ChunkEntity>> results = new ArrayList<>();

        for (Hit<ChunkEntity> hit : response.hits().hits()) {
            SearchResult<ChunkEntity> result = new SearchResult();
            result.setContent(hit.source());
            result.setScore(hit.score());
            results.add(result);
        }

        return results;
    }

}

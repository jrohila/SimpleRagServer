/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.client.EmbedClient;
import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jrohila.simpleragserver.service.util.SearchResult;
import io.github.jrohila.simpleragserver.service.util.SearchResultMapper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.domain.PageRequest;
// Note: NativeSearchQueryBuilder isn't available in this project's dependencies; we'll use StringQuery.

/**
 *
 * @author Jukka
 */
@Service
public class ChunkSearchService {

    @Value("${chunks.index-name}")
    private String indexName;

    @Autowired
    private OpenSearchClient openSearchClient;

    private static final Logger log = LoggerFactory.getLogger(ChunkSearchService.class);

    @Autowired
    private EmbedClient embedClient;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    // Limit the number of boosted terms added to the query
    private static final int MAX_TERMS = 12;

    public enum MatchType {
        MATCH, MATCH_PHRASE, MATCH_BOOL_PREFIX, QUERY_STRING, SIMPLE_QUERY_STRING
    };

    public List<SearchResult<ChunkEntity>> vectorSearch(String query, List<SearchTerm> terms, int size, String language) {
        try {
            // Build embedding for kNN
            List<Float> embedding = embedClient.getEmbeddingAsList(query);
            int k = Math.max(1, size);

            // Build filter clauses from language and mandatory terms
            List<Map<String, Object>> filters = new ArrayList<>();
            if (language != null && !language.isBlank()) {
                filters.add(Map.of("term", Map.of("language", language)));
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
                    // Use match_phrase on text to avoid relying on text.keyword mapping
                    filters.add(Map.of("match_phrase", Map.of("text", Map.of("query", raw))));
                    applied++;
                }
                if (seen.size() > MAX_TERMS) {
                    try {
                        log.info("vectorSearch: filter terms truncated to {} (from {}) due to MAX_TERMS cap", MAX_TERMS, seen.size());
                    } catch (Exception ignore) {
                    }
                }
            }

            // knn clause placed under bool.must as per example
            Map<String, Object> knn = Map.of(
                    "knn",
                    Map.of(
                            "embedding",
                            Map.of(
                                    "vector", embedding,
                                    "k", k
                            )
                    )
            );

            Map<String, Object> bool = new LinkedHashMap<>();
            bool.put("must", knn);
            if (!filters.isEmpty()) {
                bool.put("filter", filters);
            }

            Map<String, Object> queryNode = Map.of("bool", bool);

            String queryJsonContent = new ObjectMapper().writeValueAsString(queryNode);

            StringQuery stringQuery = new StringQuery(queryJsonContent);
            stringQuery.setPageable(PageRequest.of(0, size));

            log.info("Vector query (with filters): {}", queryJsonContent);
            
            return SearchResultMapper.processSearchHits(elasticsearchOperations.search(stringQuery, ChunkEntity.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute vector search", e);
        }
    }

    // Note: client-side rescoring removed for simplicity; results are returned as-is from kNN.
    // (Optional cap can be added later if needed)
    /**
     * Hybrid search combining lexical and kNN.
     *
     * Notes: - enableFuzziness applies only to MATCH queries on the "text"
     * field; others are unchanged.
     */
    public List<SearchResult<ChunkEntity>> hybridSearch(String query, MatchType matchType, List<SearchTerm> terms, int size, boolean enableFuzziness, String language) {
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

            return SearchResultMapper.processSearchResponse(resp);
        } catch (IOException | OpenSearchException e) {
            throw new RuntimeException("Failed to execute hybrid search (OpenSearch client)", e);
        }
    }

    public List<SearchResult<ChunkEntity>> lexicalSearch(String query, MatchType matchType, List<SearchTerm> terms, int size, boolean enableFuzziness, String language) {
        try {
            // 1) Build base lexical clause
            Map<String, Object> baseTextClause;
            switch (matchType) {
                case MATCH -> {
                    if (enableFuzziness) {
                        Map<String, Object> matchNode = new LinkedHashMap<>();
                        Map<String, Object> textNode = new LinkedHashMap<>();
                        textNode.put("query", query);
                        textNode.put("fuzziness", "AUTO");
                        textNode.put("prefix_length", 1);
                        textNode.put("max_expansions", 50);
                        matchNode.put("text", textNode);
                        baseTextClause = Map.of("match", matchNode);
                    } else {
                        baseTextClause = Map.of("match", Map.of("text", query));
                    }
                }
                case MATCH_PHRASE ->
                    baseTextClause = Map.of("match_phrase", Map.of("text", query));
                case MATCH_BOOL_PREFIX ->
                    baseTextClause = Map.of("match_bool_prefix", Map.of("text", query));
                case QUERY_STRING ->
                    baseTextClause = Map.of("query_string", Map.of("query", query));
                case SIMPLE_QUERY_STRING ->
                    baseTextClause = Map.of("simple_query_string", Map.of("query", query));
                default ->
                    baseTextClause = Map.of("match", Map.of("text", query));
            }

            // 2) Collect boosts and filters (language + mandatory terms)
            List<Map<String, Object>> shouldBoost = new ArrayList<>();
            List<Map<String, Object>> filters = new ArrayList<>();

            if (language != null && !language.isBlank()) {
                filters.add(Map.of("term", Map.of("language", language)));
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
                    Map<String, Object> textNode = new LinkedHashMap<>();
                    textNode.put("query", term);
                    if (t.getBoostWeight() != null) {
                        textNode.put("boost", t.getBoostWeight());
                    }
                    shouldBoost.add(Map.of("match_phrase", Map.of("text", textNode)));

                    // Mandatory filter
                    if (t.isMandatory()) {
                        filters.add(Map.of("match_phrase", Map.of("text", Map.of("query", term))));
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

            // 3) Build bool query: must baseTextClause, should boosts, filter mandatory terms/lang
            Map<String, Object> bool = new LinkedHashMap<>();
            List<Map<String, Object>> mustList = new ArrayList<>();
            mustList.add(baseTextClause);
            bool.put("must", mustList);
            if (!shouldBoost.isEmpty()) {
                bool.put("should", shouldBoost);
            }
            if (!filters.isEmpty()) {
                bool.put("filter", filters);
            }

            Map<String, Object> topQuery = Map.of("bool", bool);

            String rawJson = new ObjectMapper().writeValueAsString(topQuery);
            StringQuery stringQuery = new StringQuery(rawJson);
            stringQuery.setPageable(PageRequest.of(0, size));

            log.info("Lexical query: {}", rawJson);

            return SearchResultMapper.processSearchHits(elasticsearchOperations.search(stringQuery, ChunkEntity.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute lexical search", e);
        }
    }

}

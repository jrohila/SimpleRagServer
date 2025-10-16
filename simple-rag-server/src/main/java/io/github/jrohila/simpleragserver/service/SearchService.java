package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.client.EmbedClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import io.github.jrohila.simpleragserver.dto.SearchResultDTO;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap; // added
import java.util.LinkedHashSet; // added
import io.github.jrohila.simpleragserver.chat.util.BoostTerm; // new
import io.github.jrohila.simpleragserver.service.NlpService.NlpEngine;
import org.springframework.beans.factory.annotation.Autowired;
import io.github.jrohila.simpleragserver.util.VectorClustering;
import java.util.AbstractMap; // added
import java.util.Locale;      // added
import java.util.Set;         // added

@Service
public class SearchService {

    @Autowired
    private SummarizerService summarizerService;
    
    private final RestTemplate restTemplate;
    private final EmbedClient embedService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NlpService nlpService;
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    // Limit the number of boosted terms added to the query
    private static final int MAX_BOOST_TERMS = 12;

    public enum MatchType {
        MATCH, MATCH_PHRASE, MATCH_BOOL_PREFIX, QUERY_STRING, SIMPLE_QUERY_STRING
    };

    @Value("${opensearch.uris}")
    private String uris;

    @Value("${opensearch.username}")
    private String username;

    @Value("${opensearch.password}")
    private String password;

    @Value("${chunks.index-name}")
    private String indexName;

    public SearchService(EmbedClient embedService, NlpService nlpService) {
        this.restTemplate = new RestTemplate();
        this.embedService = embedService;
        this.nlpService = nlpService;
    }

    // Existing method kept for compatibility – delegates with null extra terms
    public Map<String, Object> searchAsRaw(
            String textQuery,
            MatchType matchType,
            boolean useKnn,
            int size
    ) throws Exception {
        return searchAsRaw(
                textQuery,
                matchType,
                useKnn,
                size,
                null // additionalBoostTerms
        );
    }

    // Remove the old addBoostTerms use; build a single clause from pairs
    // New: build a single boosted clause from extracted + provided terms
    private Map<String, Object> buildBoostClause(String textQuery, List<BoostTerm> additionalBoostTerms) {
        final int cap = Math.max(0, MAX_BOOST_TERMS);
        if (cap == 0) return null;

        final String qLower = textQuery == null ? "" : textQuery.toLowerCase(Locale.ROOT);

        // 1) Collect pairs (term, boost) with extracted first (boost=3.0), then provided (scaled), unique terms only.
        List<AbstractMap.SimpleEntry<String, Double>> pairs = new ArrayList<>(cap);
        Set<String> seen = new LinkedHashSet<>(); // case-insensitive uniqueness
        int applied = 0;

        // Extracted terms first (always with boost 3.0)
        try {
            List<String> extracted = nlpService.extractCandidateTerms(textQuery, NlpEngine.STANFORD_CORE_NLP);
            if (extracted != null) {
                for (String term : extracted) {
                    if (applied >= cap) break;
                    if (term == null || term.isBlank()) continue;
                    String key = term.toLowerCase(Locale.ROOT);
                    if (!seen.add(key)) continue;

                    pairs.add(new AbstractMap.SimpleEntry<>(term, 3.0));
                    applied++;
                }
            }
        } catch (Exception e) {
            try { log.warn("buildBoostClause: extraction failed: {}", e.getMessage()); } catch (Exception ignore) {}
        }

        if (applied < cap) {
            // Provided BoostTerms next; skip duplicates already added
            List<BoostTerm> effective = (additionalBoostTerms == null) ? List.of() : additionalBoostTerms;

            // Compute global min/max for scaling
            int globalMin = Integer.MAX_VALUE, globalMax = Integer.MIN_VALUE;
            for (BoostTerm bt : effective) {
                if (bt == null) continue;
                int c = Math.max(1, bt.getCount());
                if (c < globalMin) globalMin = c;
                if (c > globalMax) globalMax = c;
            }
            if (globalMin == Integer.MAX_VALUE) { globalMin = 1; globalMax = 1; }
            int denom = Math.max(1, globalMax - globalMin);

            for (BoostTerm bt : effective) {
                if (applied >= cap) break;
                if (bt == null) continue;
                String term = bt.getTerm();
                if (term == null || term.isBlank()) continue;

                String key = term.toLowerCase(Locale.ROOT);
                if (!seen.add(key)) continue; // skip duplicates

                boolean inQuery = !qLower.isEmpty() && qLower.contains(key);
                double boost = inQuery ? 3.0 : 1.0 + 2.0 * ((Math.max(1, bt.getCount()) - globalMin) / (double) denom);
                if (boost < 1.0) boost = 1.0;
                if (boost > 3.0) boost = 3.0;

                pairs.add(new AbstractMap.SimpleEntry<>(term, boost));
                applied++;
            }
        }

        if (pairs.isEmpty()) return null;

        // 2) Build ONE inner should with all boosted match_phrase queries
        List<Map<String, Object>> innerShould = new ArrayList<>(pairs.size());
        List<String> dbg = new ArrayList<>(pairs.size());
        for (AbstractMap.SimpleEntry<String, Double> p : pairs) {
            String term = p.getKey();
            double boost = p.getValue();
            innerShould.add(Map.of(
                "match_phrase", Map.of(
                    "text", Map.of(
                        "query", term,
                        "boost", boost
                    )
                )
            ));
            dbg.add(term + ":" + String.format(Locale.ROOT, "%.2f", boost));
        }
        try { log.info("buildBoostClause: boostTermsApplied count={} terms={}", pairs.size(), dbg); } catch (Exception ignore) {}

        // Return a single clause (nested bool/should) to be added once into top-level shouldClauses
        Map<String, Object> innerBool = new HashMap<>();
        innerBool.put("should", innerShould);
        // minimum_should_match omitted -> any boosted term contributes
        return Map.of("bool", innerBool);
    }

    // New overload: accepts additionalBoostTerms (BoostTerm objects) and merges with NLP terms
    public Map<String, Object> searchAsRaw(
            String textQuery,
            MatchType matchType,
            boolean useKnn,
            int size,
            List<BoostTerm> additionalBoostTerms
    ) throws Exception {
        // Log incoming text query
        try { log.info("searchAsRaw: textQuery='{}'", textQuery); } catch (Exception ignore) {}

        // Get embedding vector from EmbedService
        List<Float> embedding = embedService.getEmbeddingAsList(textQuery);

        // Build lexical part as a single bool/should query to avoid exceeding hybrid sub-query limits
        List<Map<String, Object>> queries = new ArrayList<>();
        List<Map<String, Object>> shouldClauses = new ArrayList<>();

        // Base text clause (from matchType)
        switch (matchType) {
            case MATCH -> shouldClauses.add(Map.of("match", Map.of("text", textQuery)));
            case MATCH_PHRASE -> shouldClauses.add(Map.of("match_phrase", Map.of("text", textQuery)));
            case MATCH_BOOL_PREFIX -> shouldClauses.add(Map.of("match_bool_prefix", Map.of("text", textQuery)));
            case QUERY_STRING -> shouldClauses.add(Map.of("query_string", Map.of("query", textQuery)));
            case SIMPLE_QUERY_STRING -> shouldClauses.add(Map.of("simple_query_string", Map.of("query", textQuery)));
        }

        // Add exactly ONE boosted clause (extracted terms first, then provided)
        Map<String, Object> boostClause = buildBoostClause(textQuery, additionalBoostTerms);
        if (boostClause != null) {
            shouldClauses.add(boostClause);
        } else {
            try { log.info("buildBoostClause: no boost terms available"); } catch (Exception ignore) {}
        }

        // Wrap lexical clauses into one bool should query (counts as a single hybrid sub-query)
        Map<String, Object> boolInner = new HashMap<>();
        boolInner.put("should", shouldClauses);
        boolInner.put("minimum_should_match", 1);
        Map<String, Object> lexicalQuery = Map.of("bool", boolInner);
        queries.add(lexicalQuery);

        // Optional semantic (kNN) as the only other sub-query
        if (useKnn) {
            queries.add(Map.of("knn", Map.of("embedding", Map.of("vector", embedding, "k", 50))));
        }

        // Build hybrid query
        Map<String, Object> hybridQuery = Map.of("hybrid", Map.of("queries", queries));

        Map<String, Object> body = Map.of("size", size, "query", hybridQuery);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (username != null && password != null) {
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);
        }

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        String base = uris.split(",")[0].trim();
        String searchEndpoint = String.format(
            "%s/%s/_search?search_pipeline=rrf-pipeline",
            base.endsWith("/") ? base.substring(0, base.length() - 1) : base,
            indexName
        );

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            searchEndpoint, HttpMethod.POST, entity,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        return response.getBody();
    }

    // Existing search() preserved and delegates to the new overload with null extras
    public List<SearchResultDTO> search(String textQuery,
            MatchType matchType,
            boolean useKnn,
            int size) throws Exception {
        Map<String, Object> body = searchAsRaw(textQuery, matchType, useKnn, size, null);
        return SearchResponseMapper.toResults(body);
    }

    // Optional new overload for callers that have extra boost terms
    public List<SearchResultDTO> search(String textQuery,
            MatchType matchType,
            boolean useKnn,
            int size,
            List<BoostTerm> additionalBoostTerms) throws Exception {
        Map<String, Object> body = searchAsRaw(textQuery, matchType, useKnn, size, additionalBoostTerms);
        return SearchResponseMapper.toResults(body);
    }

    public String searchAndSummarize(
            String textQuery,
            MatchType matchType,
            boolean useKnn,
            int size,
            int summaryLength
    ) throws Exception {
        List<SearchResultDTO> dtos = this.search(textQuery, matchType, useKnn, size);
        StringBuilder content = new StringBuilder();
        for (SearchResultDTO dto : dtos) {
            content.append(dto.getText()).append(" ");
        }
        return summarizerService.summarize(content.toString(), summaryLength);
    }

    public String searchAndSummarize(
            String textQuery,
            MatchType matchType,
            boolean useKnn,
            int size,
            int summaryLength,
            String method
    ) throws Exception {
        List<SearchResultDTO> dtos = this.search(textQuery, matchType, useKnn, size);
        StringBuilder content = new StringBuilder();
        for (SearchResultDTO dto : dtos) {
            content.append(dto.getText()).append(" ");
        }
        if (method == null || method.isBlank()) {
            return summarizerService.summarize(content.toString(), summaryLength);
        }
    SummarizerService.Method m;
    try { m = SummarizerService.Method.valueOf(method.trim().toUpperCase()); } catch (Exception e) { m = null; }
    return (m == null)
        ? summarizerService.summarize(content.toString(), summaryLength)
        : summarizerService.summarize(content.toString(), summaryLength, m);
    }

    public String searchAndSummarize(
        String textQuery,
        MatchType matchType,
        boolean useKnn,
        int size,
        int summaryLength,
        SummarizerService.Method method
    ) throws Exception {
    List<SearchResultDTO> dtos = this.search(textQuery, matchType, useKnn, size);
    StringBuilder content = new StringBuilder();
    for (SearchResultDTO dto : dtos) {
        content.append(dto.getText()).append(" ");
    }
    return (method == null)
        ? summarizerService.summarize(content.toString(), summaryLength)
        : summarizerService.summarize(content.toString(), summaryLength, method);
    }

    /**
     * Search for chunks and cluster them by their embedding vectors.
     * If minPts or eps are null, sensible defaults/auto-epsilon are used.
     */
    public List<List<SearchResultDTO>> searchAndCluster(
            String textQuery,
            MatchType matchType,
            boolean useKnn,
            int size,
            Integer minPts,
            Double eps
    ) throws Exception {
        List<SearchResultDTO> dtos = this.search(textQuery, matchType, useKnn, size);
        return VectorClustering.dbscanAutoCosine(dtos, minPts, eps);
    }

}

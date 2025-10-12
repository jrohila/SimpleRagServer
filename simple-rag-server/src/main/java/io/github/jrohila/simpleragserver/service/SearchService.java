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

@Service
public class SearchService {

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

    public Map<String, Object> searchAsRaw(
            String textQuery,
            MatchType matchType,
            boolean useKnn,
            int size
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
            case MatchType.MATCH ->
                shouldClauses.add(Map.of("match", Map.of("text", textQuery)));
            case MatchType.MATCH_PHRASE ->
                shouldClauses.add(Map.of("match_phrase", Map.of("text", textQuery)));
            case MatchType.MATCH_BOOL_PREFIX ->
                shouldClauses.add(Map.of("match_bool_prefix", Map.of("text", textQuery)));
            case MatchType.QUERY_STRING ->
                shouldClauses.add(Map.of("query_string", Map.of("query", textQuery)));
            case MatchType.SIMPLE_QUERY_STRING ->
                shouldClauses.add(Map.of("simple_query_string", Map.of("query", textQuery)));
        }

        // Extract boost terms using NLP and add them as additional clauses inside the same bool query
        try {
            List<String> terms = nlpService.extractCandidateTerms(textQuery);
            if (terms != null && !terms.isEmpty()) {
                int cap = Math.min(terms.size(), MAX_BOOST_TERMS); // cap to avoid overly large bool
                List<String> usedTerms = new ArrayList<>();
                for (int i = 0; i < cap; i++) {
                    String term = terms.get(i);
                    if (term == null || term.isBlank()) continue;
                    usedTerms.add(term);
                    // Boosted phrase match for the term
                    Map<String, Object> boosted = Map.of(
                        "match_phrase", Map.of(
                            "text", Map.of(
                                "query", term,
                                "boost", 2.5
                            )
                        )
                    );
                    shouldClauses.add(boosted);
                }
                try { log.info("searchAsRaw: boostTermsApplied count={} terms={}", usedTerms.size(), usedTerms); } catch (Exception ignore) {}
            }
        } catch (RuntimeException ignore) {
            // If NLP fails, skip boosts silently
            try { log.warn("searchAsRaw: boost term extraction failed: {}", ignore.getMessage()); } catch (Exception e2) {}
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
        Map<String, Object> hybridQuery = Map.of(
                "hybrid", Map.of(
                        "queries", queries
                )
        );

        Map<String, Object> body = Map.of(
                "size", size,
                "query", hybridQuery
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Get OpenSearch connection info from environment
        if (username != null && password != null) {
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);
        }

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        String base = uris.split(",")[0].trim();
        //String searchEndpoint = String.format("%s/%s/_search", base.endsWith("/") ? base.substring(0, base.length() - 1) : base, indexName);
        String searchEndpoint = String.format("%s/%s/_search?search_pipeline=rrf-pipeline", base.endsWith("/") ? base.substring(0, base.length() - 1) : base, indexName);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                searchEndpoint,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
        }
        );

        return response.getBody();
    }

    public List<SearchResultDTO> search(String textQuery,
            MatchType matchType,
            boolean useKnn,
            int size) throws Exception {
        Map<String, Object> body = searchAsRaw(textQuery, matchType, useKnn, size);
        return SearchResponseMapper.toResults(body);
    }

}

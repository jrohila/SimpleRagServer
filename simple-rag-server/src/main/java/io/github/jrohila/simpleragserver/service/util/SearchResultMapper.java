/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.service.util;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import java.util.ArrayList;
import java.util.List;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

/**
 *
 * @author Jukka
 */
public class SearchResultMapper {

    public static List<SearchResult<ChunkEntity>> processSearchHits(SearchHits<ChunkEntity> searchHits) {
        List<SearchResult<ChunkEntity>> results = new ArrayList<>();

        for (SearchHit<ChunkEntity> hit : searchHits.getSearchHits()) {
            SearchResult<ChunkEntity> result = new SearchResult();
            result.setContent(hit.getContent());
            result.setScore(hit.getScore());
            results.add(result);
        }

        return results;
    }

    public static List<SearchResult<ChunkEntity>> processSearchResponse(SearchResponse<ChunkEntity> response) {
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

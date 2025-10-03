package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.dto.ReferenceDTO;
import io.github.jrohila.simpleragserver.dto.SearchResultDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps OpenSearch _search response bodies (as Map) to a list of
 * SearchResultDTOs.
 */
public class SearchResponseMapper {

    @SuppressWarnings("unchecked")
    public static List<SearchResultDTO> toResults(Map<String, Object> responseBody) {
        List<SearchResultDTO> results = new ArrayList<>();
        if (responseBody == null) {
            return results;
        }

        Object hitsObj = responseBody.get("hits");
        if (!(hitsObj instanceof Map)) {
            return results;
        }
        Map<String, Object> hitsMap = (Map<String, Object>) hitsObj;

        Object innerHitsObj = hitsMap.get("hits");
        if (!(innerHitsObj instanceof List)) {
            return results;
        }

        List<Map<String, Object>> innerHits = (List<Map<String, Object>>) innerHitsObj;
        for (Map<String, Object> hit : innerHits) {
            SearchResultDTO dto = new SearchResultDTO();
            Object scoreObj = hit.get("_score");
            if (scoreObj instanceof Number) {
                dto.setScore(((Number) scoreObj).floatValue());
            }

            Object sourceObj = hit.get("_source");
            if (sourceObj instanceof Map) {
                Map<String, Object> source = (Map<String, Object>) sourceObj;
                // Text content
                Object text = source.get("text");
                if (text instanceof String) {
                    dto.setText((String) text);
                }

                // Reference details
                ReferenceDTO ref = new ReferenceDTO();

                Object docId = source.get("documentId");
                if (docId instanceof String) {
                    ref.setDocumentId((String) docId);
                }

                Object docName = source.get("documentName");
                if (docName instanceof String) {
                    ref.setDocumentName((String) docName);
                }

                Object sectionTitle = source.get("sectionTitle");
                if (sectionTitle instanceof String) {
                    ref.setSectionTitle((String) sectionTitle);
                }

                Object pageNumber = source.get("pageNumber");
                if (pageNumber instanceof Number) {
                    ref.setPageNumber(((Number) pageNumber).intValue());
                }

                // documentName and url are not present in the chunk doc; leave null for now
                dto.setReference(ref);
            }

            results.add(dto);
        }

        return results;
    }
}

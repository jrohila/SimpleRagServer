/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.controller.util;

import io.github.jrohila.simpleragserver.dto.ReferenceDTO;
import io.github.jrohila.simpleragserver.dto.SearchResultDTO;
import io.github.jrohila.simpleragserver.entity.ChunkEntity;

/**
 *
 * @author Jukka
 */
public class SearchResultDtoMapper {

    public static SearchResultDTO mapChunkEntity(ChunkEntity e, float score) {
        SearchResultDTO dto = new SearchResultDTO();
        dto.setScore((float) score);
        dto.setText(e.getText());
        dto.setEmbedding(e.getEmbedding());
        ReferenceDTO ref = new ReferenceDTO();
        ref.setDocumentId(e.getDocumentId());
        ref.setDocumentName(e.getDocumentName());
        ref.setSectionTitle(e.getSectionTitle());
        ref.setPageNumber(e.getPageNumber());
        dto.setReference(ref);
        return dto;
    }

}

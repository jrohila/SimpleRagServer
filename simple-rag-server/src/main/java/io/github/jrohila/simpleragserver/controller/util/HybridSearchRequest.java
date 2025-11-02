/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.controller.util;

import io.github.jrohila.simpleragserver.controller.SearchController;
import io.github.jrohila.simpleragserver.repository.ChunkSearchService;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author Jukka
 */
// Request payloads for hybrid search
@Getter
@Setter
@ToString
public class HybridSearchRequest {

    private String query;
    private ChunkSearchService.MatchType matchType;
    private Integer size;
    private List<Term> terms;
    private Boolean enableFuzziness;
    private String language;

}

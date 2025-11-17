/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author Jukka
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkingTaskEntity {

    private String id;
    
    private String collectionId;
    
    private String documentId;
    
    private String taskId;
    
    private DocumentEntity.ProcessingState status;
    
    private String created;

    private String modified;

}
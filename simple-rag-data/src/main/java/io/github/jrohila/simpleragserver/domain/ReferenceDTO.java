/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author Lenovo
 */
@Getter
@Setter
@ToString
public class ReferenceDTO {
    
    private String documentId;
    private String documentName;
    private String sectionTitle;
    private int pageNumber;
    private String url;
    
}

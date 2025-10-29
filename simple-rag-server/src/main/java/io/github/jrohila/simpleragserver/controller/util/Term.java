/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.controller.util;

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
public class Term {

    private String term;
    private Double boostWeight;
    private Boolean mandatory;

}

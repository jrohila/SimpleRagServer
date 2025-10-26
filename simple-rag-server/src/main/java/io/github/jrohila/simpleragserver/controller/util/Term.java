/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.controller.util;

/**
 *
 * @author Jukka
 */
public class Term {

    private String term;
    private Double boostWeight;
    private Boolean mandatory;

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public Double getBoostWeight() {
        return boostWeight;
    }

    public void setBoostWeight(Double boostWeight) {
        this.boostWeight = boostWeight;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }
}

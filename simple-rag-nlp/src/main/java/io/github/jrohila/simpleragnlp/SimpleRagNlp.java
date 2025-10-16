/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package io.github.jrohila.simpleragnlp;

import io.github.jrohila.simpleragnlp.impl.TermFinderSCNImpl;

/**
 *
 * @author Jukka
 */
public class SimpleRagNlp {

    public static void main(String[] args) {
        System.out.println("Hello World!");
        
        System.out.println(TermFinderSCNImpl.extractTerms("What kind of career an ENTP person should have?"));
        
    }
}

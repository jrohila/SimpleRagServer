/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.dto;

/**
 *
 * @author Lenovo
 */
public class Chunk {

    public String text;
    public String type;
    public String sectionTitle;
    public int pageNumber;
    public String language;

    public Chunk(String text, String type, String sectionTitle, int pageNumber, String language) {
        this.text = text;
        this.type = type;
        this.sectionTitle = sectionTitle;
        this.pageNumber = pageNumber;
        this.language = language;
    }

    @Override
    public String toString() {
        return "Chunk[type=" + type + ", sectionTitle=" + sectionTitle
                + ", page=" + pageNumber + ", language=" + language
                + ", text=\"" + (text.length() > 30 ? text.substring(0, 30) + "..." : text) + "\"]";
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.domain;

import java.util.Map;
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
public class SourceInput {

    private String kind; // "http" | "file" | "s3"

    // For kind=http
    private String url;
    private Map<String, Object> headers;

    // For kind=file
    private String filename;
    private String base64String;
}

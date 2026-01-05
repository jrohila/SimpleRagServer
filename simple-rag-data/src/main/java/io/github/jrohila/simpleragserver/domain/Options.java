/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.domain;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Options {

    private List<String> toFormats; // e.g. ["json","md"]
    private List<String> fromFormats;
    private String imageExportMode; // embedded|placeholder|referenced
    private Boolean doOcr;
    private Boolean forceOcr;
    private String ocrEngine;
    private String pdfBackend;
    private String tableMode; // fast|accurate
    private Boolean tableCellMatching;
    private String pipeline; // standard|vlm|asr
    private List<Integer> pageRange;
    private Double documentTimeout;
    private Boolean abortOnError;
    private Boolean doTableStructure;
    private Boolean includeImages;
    private Double imagesScale;
    private String mdPageBreakPlaceholder;

    public static Options defaultForRag() {
        Options o = new Options();
        o.setToFormats(List.of("json", "md"));
        o.setDoOcr(true);
        o.setPipeline("standard");
        o.setDoTableStructure(true);
        o.setTableMode("fast");
        o.setAbortOnError(false);
        // leave other fields to server defaults
        return o;
    }
}

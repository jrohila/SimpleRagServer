/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import io.github.jrohila.simpleragserver.pipeline.PdfToXhtmlConversion;
import io.github.jrohila.simpleragserver.pipeline.XhtmlCleanup;
import io.github.jrohila.simpleragserver.pipeline.XhtmlToChunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/processing")
public class ProcessingController {

    @Autowired
    private PdfToXhtmlConversion pdfToXhtmlConversionService;
    @Autowired
    private XhtmlCleanup xhtmlStreamLineService;
    @Autowired
    private XhtmlToChunk xhtmlToChunkService;

    @PostMapping(value = "/extract", consumes = "multipart/form-data")
    public ResponseEntity<List<ChunkEntity>> extractChunks(@RequestParam("file") MultipartFile file) throws IOException {
        String xhtml = pdfToXhtmlConversionService.parseToXhtml(file.getBytes());
        String streamlined = xhtmlStreamLineService.streamlineParagraphs(xhtml);
        List<ChunkEntity> chunks = xhtmlToChunkService.parseChunks(streamlined);
        return ResponseEntity.ok(chunks);
    }

    @PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> parsePdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "streamLineXhtml", required = false, defaultValue = "true") boolean streamLineXhtml
    ) {
        try {
            byte[] bytes = file.getBytes();
            String xhtml = pdfToXhtmlConversionService.parseToXhtml(bytes);
            if (streamLineXhtml) {
                xhtml = xhtmlStreamLineService.streamlineParagraphs(xhtml);
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(xhtml);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("<error>" + e.getMessage() + "</error>");
        }
    }

}

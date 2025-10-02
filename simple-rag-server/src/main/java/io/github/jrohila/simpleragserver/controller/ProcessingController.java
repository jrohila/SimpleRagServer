/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.pipeline.DocumentChunker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/processing")
public class ProcessingController {

    @Autowired
    private DocumentChunker documentChunker;

    @PostMapping("/chunk-document/{documentId}")
    public ResponseEntity<Void> processDocumentSync(@PathVariable String documentId) {
        documentChunker.process(documentId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/chunk-document/async/{documentId}")
    public ResponseEntity<Void> processDocumentAsync(@PathVariable String documentId) {
        documentChunker.asyncProcess(documentId);
        return ResponseEntity.accepted().build();
    }

}

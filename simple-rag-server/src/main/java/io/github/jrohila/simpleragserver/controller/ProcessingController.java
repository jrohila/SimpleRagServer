/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.service.DocumentChunkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/processing")
public class ProcessingController {

    @Autowired
    private DocumentChunkerService documentChunker;

    @PostMapping("/chunk-document/{collectionId}/{documentId}")
    public ResponseEntity<Void> processDocumentSync(@PathVariable String collectionId, @PathVariable String documentId) {
        documentChunker.process(collectionId, documentId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/chunk-document/async/{collectionId}/{documentId}")
    public ResponseEntity<Void> processDocumentAsync(@PathVariable String collectionId, @PathVariable String documentId) {
        documentChunker.asyncProcess(collectionId, documentId);
        return ResponseEntity.accepted().build();
    }

}

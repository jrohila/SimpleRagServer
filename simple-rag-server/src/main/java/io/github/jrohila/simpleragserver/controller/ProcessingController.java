/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.service.DocumentChunkerService;
import jakarta.inject.Inject;
import io.micronaut.http.annotation.Controller;

@Controller("/api/processing")
public class ProcessingController {

    @Inject
    private DocumentChunkerService documentChunker;
/*
    @Post(uri = "/chunk-document/{collectionId}/{documentId}")
    public void processDocumentSync(@PathVariable String collectionId, @PathVariable String documentId) {
        documentChunker.process(collectionId, documentId);
    }

    @Post(uri = "/chunk-document/async/{collectionId}/{documentId}")
    public void processDocumentAsync(@PathVariable String collectionId, @PathVariable String documentId) {
        documentChunker.asyncProcess(collectionId, documentId);
    }*/

}

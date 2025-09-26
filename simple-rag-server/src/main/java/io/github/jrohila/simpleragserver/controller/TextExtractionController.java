package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.dto.Chunk;
import io.github.jrohila.simpleragserver.service.TextExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/text-extraction")
public class TextExtractionController {
    @Autowired
    private TextExtractionService textExtractionService;

    @PostMapping(value = "/extract-text", consumes = "multipart/form-data")
    public ResponseEntity<String> extractText(@RequestParam("file") MultipartFile file) {
        try {
            String text = textExtractionService.extractText(file.getBytes());
            return ResponseEntity.ok(text);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error extracting text: " + e.getMessage());
        }
    }

    @PostMapping(value = "/extract-paragraphs", consumes = "multipart/form-data")
    public ResponseEntity<Chunk[]> extractParagraphs(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", defaultValue = "PARAGRAPHS_FROM_RAW_TEXT") TextExtractionService.ParagraphExtractionMode mode
    ) {
        try {
            Chunk[] paragraphs = textExtractionService.extractParagraphs(file.getBytes(), mode);
            return ResponseEntity.ok(paragraphs);
        } catch (Exception e) {
            return null;
        }
    }
}

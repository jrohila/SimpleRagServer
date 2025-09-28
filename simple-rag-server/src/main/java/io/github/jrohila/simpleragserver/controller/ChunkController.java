package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.model.ChunkDto;
import io.github.jrohila.simpleragserver.service.PdfToXhtmlConversionService;
import io.github.jrohila.simpleragserver.service.XhtmlStreamLineService;
import io.github.jrohila.simpleragserver.service.XhtmlToChunkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/chunks")
public class ChunkController {
    @Autowired
    private PdfToXhtmlConversionService pdfToXhtmlConversionService;
    @Autowired
    private XhtmlStreamLineService xhtmlStreamLineService;
    @Autowired
    private XhtmlToChunkService xhtmlToChunkService;

    @PostMapping(value = "/extract", consumes = "multipart/form-data")
    public ResponseEntity<List<ChunkDto>> extractChunks(@RequestParam("file") MultipartFile file) throws IOException {
        String xhtml = pdfToXhtmlConversionService.parseToXhtml(file.getBytes());
        String streamlined = xhtmlStreamLineService.streamlineParagraphs(xhtml);
        List<ChunkDto> chunks = xhtmlToChunkService.parseChunks(streamlined);
        return ResponseEntity.ok(chunks);
    }
}

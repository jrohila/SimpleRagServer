package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.service.PdfToXhtmlConversionService;
import io.github.jrohila.simpleragserver.service.XhtmlStreamLineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/parse")
public class DocumentParseController {

    private final PdfToXhtmlConversionService pdfToXhtmlConversionService;
        private final XhtmlStreamLineService xhtmlStreamLineService;

    @Autowired
        public DocumentParseController(PdfToXhtmlConversionService pdfToXhtmlConversionService, XhtmlStreamLineService xhtmlStreamLineService) {
        this.pdfToXhtmlConversionService = pdfToXhtmlConversionService;
            this.xhtmlStreamLineService = xhtmlStreamLineService;
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

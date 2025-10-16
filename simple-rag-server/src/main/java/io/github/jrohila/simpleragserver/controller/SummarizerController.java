package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.service.SummarizerService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "/v1/summarize")
public class SummarizerController {

    private final SummarizerService summarizerService;

    public SummarizerController(SummarizerService summarizerService) {
        this.summarizerService = summarizerService;
    }

    public static class SummarizeRequest {
        public String text;
        public Integer maxWords;
        public SummarizerService.Method method; // Expose enum for Swagger UI dropdown
        public String getText() { return text; }
        public Integer getMaxWords() { return maxWords; }
        public SummarizerService.Method getMethod() { return method; }
        public void setText(String t) { this.text = t; }
        public void setMaxWords(Integer m) { this.maxWords = m; }
        public void setMethod(SummarizerService.Method m) { this.method = m; }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> summarize(@RequestBody SummarizeRequest req) {
        String text = req != null ? req.text : null;
        int max = (req != null && req.maxWords != null) ? req.maxWords : 200; // default budget
        SummarizerService.Method method = (req != null) ? req.method : null;
        String summary = (method == null)
                ? summarizerService.summarize(text, max)
                : summarizerService.summarize(text, max, method);
        return Map.of(
            "summary", summary,
            "length", summary != null ? summary.length() : 0
        );
    }
}

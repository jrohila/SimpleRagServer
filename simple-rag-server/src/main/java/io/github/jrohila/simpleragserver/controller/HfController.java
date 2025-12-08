package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.client.hf.HfModelInfo;
import io.github.jrohila.simpleragserver.client.hf.HuggingFaceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/internal/hf")
public class HfController {

    private final HuggingFaceClient hfClient;

    public HfController(HuggingFaceClient hfClient) {
        this.hfClient = hfClient;
    }

    /**
     * Returns list of ONNX-community text-generation models with available metadata.
     */
    @GetMapping("/models")
    public ResponseEntity<List<HfModelInfo>> listModels(@RequestParam(value = "page", required = false) Integer page) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("author", "onnx-community");
            params.put("pipeline_tag", "text-generation");
            params.put("limit", "100");
            params.put("sort", "downloads");
            params.put("direction", "-1");
            if (page != null) {
                params.put("p", String.valueOf(page));
            }
            List<HfModelInfo> models = hfClient.getModelsFromHub(params);
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}

package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.HfModelEntity;
import io.github.jrohila.simpleragserver.repository.HfModelService;
import io.github.jrohila.simpleragserver.client.hf.HfModelInfo;
import io.github.jrohila.simpleragserver.client.hf.HuggingFaceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/internal/hf/models")
public class HfModelController {

    @Autowired
    private HfModelService hfModelService;

    @Autowired
    private HuggingFaceClient hfClient;

    @PostMapping
    public ResponseEntity<HfModelEntity> create(@RequestBody HfModelEntity model) {
        HfModelEntity created = hfModelService.create(model);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HfModelEntity> getById(@PathVariable String id) {
        return hfModelService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<HfModelEntity>> list(@RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "size", defaultValue = "30") int size) {
        List<HfModelEntity> list = hfModelService.list(page, size);
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        hfModelService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Trigger import from Hugging Face. If async=true (default), returns immediately.
     */
    @PostMapping("/import")
    public ResponseEntity<String> importAll(@RequestParam(value = "async", defaultValue = "true") boolean async) {
        if (async) {
            hfModelService.fetchAndSaveAllModelsAsync();
            return ResponseEntity.accepted().body("Import started");
        } else {
            int saved = hfModelService.fetchAndSaveAllModels();
            return ResponseEntity.ok("Import completed, saved=" + saved);
        }
    }

    /**
     * Fetch models directly from Hugging Face Hub (organization models-json). Does not persist.
     */
    @GetMapping("/hub")
    public ResponseEntity<List<HfModelInfo>> listHubModels(@RequestParam(value = "page", required = false) Integer page) {
        try {
            java.util.Map<String, String> params = new java.util.HashMap<>();
            params.put("author", "onnx-community");
            params.put("pipeline_tag", "text-generation");
            params.put("sort", "downloads");
            // models-json endpoint doesn't accept 'direction' or 'limit' - the client will strip unsupported params
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

package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.HfModelEntity;
import io.github.jrohila.simpleragserver.repository.HfModelService;
import io.github.jrohila.simpleragserver.client.hf.HfModelInfo;
import io.github.jrohila.simpleragserver.client.hf.HuggingFaceClient;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;

import java.util.List;
import jakarta.annotation.Nullable;

@Controller("/api/internal/hf/models")
public class HfModelController {

    private final HfModelService hfModelService;
    private final HuggingFaceClient hfClient;

    public HfModelController(HfModelService hfModelService, HuggingFaceClient hfClient) {
        this.hfModelService = hfModelService;
        this.hfClient = hfClient;
    }

    @Post
    public HttpResponse<HfModelEntity> create(@Body HfModelEntity model) {
        HfModelEntity created = hfModelService.create(model);
        return HttpResponse.ok(created);
    }

    @Get("/{id}")
    public HttpResponse<HfModelEntity> getById(@PathVariable String id) {
        return hfModelService.getById(id)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Get
    public HttpResponse<List<HfModelEntity>> list(@QueryValue(value = "page", defaultValue = "0") int page,
                                                   @QueryValue(value = "size", defaultValue = "30") int size) {
        List<HfModelEntity> list = hfModelService.list(page, size);
        return HttpResponse.ok(list);
    }

    @Delete("/{id}")
    public HttpResponse<Void> delete(@PathVariable String id) {
        hfModelService.deleteById(id);
        return HttpResponse.noContent();
    }

    /**
     * Trigger import from Hugging Face. If async=true (default), returns immediately.
     */
    @Post("/import")
    public HttpResponse<String> importAll(@QueryValue(value = "async", defaultValue = "true") boolean async) {
        if (async) {
            hfModelService.fetchAndSaveAllModelsAsync();
            return HttpResponse.status(HttpStatus.ACCEPTED).body("Import started");
        } else {
            int saved = hfModelService.fetchAndSaveAllModels();
            return HttpResponse.ok("Import completed, saved=" + saved);
        }
    }

    /**
     * Fetch models directly from Hugging Face Hub (organization models-json). Does not persist.
     */
    @Get("/hub")
    public HttpResponse<List<HfModelInfo>> listHubModels(@Nullable @QueryValue(value = "page") Integer page) {
        try {
            java.util.Map<String, String> params = new java.util.HashMap<>();
            params.put("author", "onnx-community");
            params.put("pipeline_tag", "text-generation");
            params.put("sort", "downloads");
            if (page != null) {
                params.put("p", String.valueOf(page));
            }
            List<HfModelInfo> models = hfClient.getModelsFromHub(params);
            return HttpResponse.ok(models);
        } catch (Exception e) {
            return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Search stored HF models by optional name (substring of model id) and optional max size in MB.
     * Returns up to 25 results by default when no filters are provided.
     */
    @Get("/search")
    public HttpResponse<List<HfModelEntity>> search(@QueryValue(value = "name", defaultValue = "") String name,
                                                     @Nullable @QueryValue(value = "sizeMb") Double sizeMb,
                                                     @QueryValue(value = "limit", defaultValue = "25") int limit) {
        try {
            List<HfModelEntity> results = hfModelService.search(name, sizeMb, limit);
            return HttpResponse.ok(results);
        } catch (Exception e) {
            return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

package io.github.jrohila.simpleragserver.controller;

import io.github.jrohila.simpleragserver.domain.CollectionEntity;
import io.github.jrohila.simpleragserver.repository.CollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller("/api/collections")
public class CollectionController {

    private static final Logger log = LoggerFactory.getLogger(CollectionController.class);

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @Post
    public HttpResponse<CollectionEntity> create(@Body CollectionEntity collection) {
        log.info("Creating new collection: name={}", collection.getName());
        CollectionEntity created = collectionService.create(collection);
        log.info("Collection created successfully: id={}, name={}", created.getId(), created.getName());
        return HttpResponse.ok(created);
    }

    @Get("/{id}")
    public HttpResponse<CollectionEntity> getById(@PathVariable String id) {
        log.debug("Fetching collection by id: {}", id);
        Optional<CollectionEntity> collection = collectionService.getById(id);
        if (collection.isPresent()) {
            log.debug("Collection found: id={}, name={}", id, collection.get().getName());
            return HttpResponse.ok(collection.get());
        } else {
            log.warn("Collection not found: id={}", id);
            return HttpResponse.notFound();
        }
    }

    @Get
    public HttpResponse<List<CollectionEntity>> list(@QueryValue(defaultValue = "0") int page,
                                                      @QueryValue(defaultValue = "10") int size) {
        log.debug("Listing collections: page={}, size={}", page, size);
        List<CollectionEntity> collections = collectionService.list(page, size);
        log.info("Retrieved {} collections", collections.size());
        return HttpResponse.ok(collections);
    }

    @Put("/{id}")
    public HttpResponse<CollectionEntity> update(@PathVariable String id, @Body CollectionEntity collection) {
        log.info("Updating collection: id={}, name={}", id, collection.getName());
        CollectionEntity updated = collectionService.update(id, collection);
        log.info("Collection updated successfully: id={}, name={}", updated.getId(), updated.getName());
        return HttpResponse.ok(updated);
    }

    @Delete("/{id}")
    public HttpResponse<Void> delete(@PathVariable String id) {
        log.info("Deleting collection: id={}", id);
        boolean deleted = collectionService.deleteById(id);
        if (deleted) {
            log.info("Collection deleted successfully: id={}", id);
            return HttpResponse.noContent();
        } else {
            log.warn("Collection not found for deletion: id={}", id);
            return HttpResponse.notFound();
        }
    }
}

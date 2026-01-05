package io.github.jrohila.simpleragserver.config;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Factory
public class AsyncConfig {

    @Singleton
    @Named("chunkingExecutor")
    public Executor chunkingExecutor() {
        return Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r);
            t.setName("chunk-" + t.getId());
            return t;
        });
    }
}
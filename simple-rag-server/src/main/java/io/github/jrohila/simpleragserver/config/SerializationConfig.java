package io.github.jrohila.simpleragserver.config;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import io.micronaut.serde.ObjectMapper;

/**
 * Configuration for Micronaut Serialization.
 * Uses Jackson under the hood but with compile-time annotation processing.
 */
@Factory
public class SerializationConfig {
    
    @Singleton
    public ObjectMapper objectMapper() {
        return ObjectMapper.getDefault();
    }
}

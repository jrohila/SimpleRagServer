package io.github.jrohila.simpleragserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for Jackson ObjectMapper bean.
 * Required for OpenSearch client which needs Jackson for JSON serialization.
 */
@Factory
public class JacksonConfig {

    private static final Logger log = LoggerFactory.getLogger(JacksonConfig.class);

    @Singleton
    public ObjectMapper objectMapper() {
        log.info("Creating Jackson ObjectMapper bean");
        ObjectMapper mapper = new ObjectMapper();
        
        // Register JavaTimeModule for Java 8 date/time types
        mapper.registerModule(new JavaTimeModule());
        
        // Configure serialization options
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        log.info("Jackson ObjectMapper configured successfully");
        return mapper;
    }
}

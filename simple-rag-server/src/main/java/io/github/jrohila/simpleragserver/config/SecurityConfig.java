package io.github.jrohila.simpleragserver.config;

import io.micronaut.context.annotation.Factory;
import io.micronaut.http.server.cors.CorsOriginConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.http.HttpMethod;

import java.util.Arrays;
import java.util.Collections;

@Factory
public class SecurityConfig {
    
    @Bean
    public CorsOriginConfiguration corsConfiguration() {
        CorsOriginConfiguration config = new CorsOriginConfiguration();
        config.setAllowedOrigins(Arrays.asList("http://localhost:8081", "http://localhost:19006"));
        config.setAllowedMethods(Arrays.asList(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.OPTIONS));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowCredentials(true);
        return config;
    }
}

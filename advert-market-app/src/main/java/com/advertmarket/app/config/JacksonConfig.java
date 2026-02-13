package com.advertmarket.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration for components that directly depend on ObjectMapper.
 */
@Configuration
public class JacksonConfig {

    /**
     * Provides a default ObjectMapper with auto-registered modules.
     *
     * @return configured object mapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }
}

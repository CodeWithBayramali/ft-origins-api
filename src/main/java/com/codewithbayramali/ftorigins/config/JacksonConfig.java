package com.codewithbayramali.ftorigins.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Boot 4 no longer exposes an ObjectMapper bean by default. This one serves the match services
 * (engine event payloads and the command log — plain strings/numbers/booleans only); the MVC layer
 * keeps its own internally-configured mapper for HTTP DTOs.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

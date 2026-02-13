package com.advertmarket.app.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Kafka client connectivity properties.
 *
 * @param bootstrapServers Kafka bootstrap servers
 */
@Validated
@ConfigurationProperties(prefix = "spring.kafka")
public record KafkaClientProperties(
        @NotBlank String bootstrapServers
) {
}

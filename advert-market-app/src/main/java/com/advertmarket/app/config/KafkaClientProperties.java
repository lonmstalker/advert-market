package com.advertmarket.app.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
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
@PropertyGroupDoc(
        displayName = "Kafka Client",
        description = "Kafka client connectivity configuration",
        category = "Messaging"
)
public record KafkaClientProperties(
        @PropertyDoc(
                description = "Comma-separated list of Kafka broker addresses (host:port)",
                required = Requirement.REQUIRED
        )
        @NotBlank String bootstrapServers
) {
}
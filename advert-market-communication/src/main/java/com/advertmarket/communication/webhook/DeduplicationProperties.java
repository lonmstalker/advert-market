package com.advertmarket.communication.webhook;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for update deduplication.
 *
 * @param ttl time-to-live for processed update ids in Redis
 */
@ConfigurationProperties(prefix = "app.telegram.deduplication")
@Validated
public record DeduplicationProperties(
        @DefaultValue("24h") Duration ttl
) {
}

package com.advertmarket.communication.webhook;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
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
@PropertyGroupDoc(
        displayName = "Update Deduplication",
        description = "Deduplication of incoming Telegram updates via Redis",
        category = "Telegram"
)
@Validated
public record DeduplicationProperties(
        @PropertyDoc(
                description = "TTL for processed update ids in Redis",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("24h") Duration ttl
) {
}

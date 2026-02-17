package com.advertmarket.shared.outbox;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the transactional outbox poller.
 */
@ConfigurationProperties(prefix = "app.outbox")
@PropertyGroupDoc(
        displayName = "Outbox Poller",
        description = "Transactional outbox polling configuration",
        category = "Outbox"
)
public record OutboxProperties(

        @PropertyDoc(
                description = "Polling interval between outbox scans",
                required = Requirement.OPTIONAL
        )
        Duration pollInterval,

        @PropertyDoc(
                description = "Maximum number of entries per poll batch",
                required = Requirement.OPTIONAL
        )
        int batchSize,

        @PropertyDoc(
                description = "Maximum number of retry attempts before marking as failed",
                required = Requirement.OPTIONAL
        )
        int maxRetries,

        @PropertyDoc(
                description = "Initial backoff duration before first retry",
                required = Requirement.OPTIONAL
        )
        Duration initialBackoff,

        @PropertyDoc(
                description = "Timeout for publishing a single outbox entry to Kafka",
                required = Requirement.OPTIONAL
        )
        Duration publishTimeout,

        @PropertyDoc(
                description = "Seconds after which a PROCESSING entry is considered stuck",
                required = Requirement.OPTIONAL
        )
        int stuckThresholdSeconds
) {
    /** Applies defaults for unset properties. */
    public OutboxProperties {
        if (pollInterval == null) {
            pollInterval = Duration.ofMillis(500);
        }
        if (batchSize <= 0) {
            batchSize = 50;
        }
        if (maxRetries <= 0) {
            maxRetries = 3;
        }
        if (initialBackoff == null) {
            initialBackoff = Duration.ofSeconds(1);
        }
        if (publishTimeout == null) {
            publishTimeout = Duration.ofSeconds(5);
        }
        if (stuckThresholdSeconds <= 0) {
            stuckThresholdSeconds = 300;
        }
    }
}

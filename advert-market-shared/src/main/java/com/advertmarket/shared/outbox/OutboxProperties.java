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
        Duration publishTimeout
) {

    private static final long DEFAULT_POLL_INTERVAL_MS = 500;
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_PUBLISH_TIMEOUT_S = 5;

    /** Applies defaults for unset properties. */
    public OutboxProperties {
        if (pollInterval == null) {
            pollInterval = Duration.ofMillis(DEFAULT_POLL_INTERVAL_MS);
        }
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        if (maxRetries <= 0) {
            maxRetries = DEFAULT_MAX_RETRIES;
        }
        if (initialBackoff == null) {
            initialBackoff = Duration.ofSeconds(1);
        }
        if (publishTimeout == null) {
            publishTimeout = Duration.ofSeconds(DEFAULT_PUBLISH_TIMEOUT_S);
        }
    }
}

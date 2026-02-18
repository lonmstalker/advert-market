package com.advertmarket.marketplace.channel.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for channel statistics background collection.
 *
 * @param enabled enables/disables periodic collection
 * @param batchSize maximum channels processed per cycle
 * @param retryBackoffMs fixed backoff between retries for transient failures
 * @param maxRetriesPerChannel max retries after the first attempt for each channel
 * @param adminCheckInterval minimum interval between admin list checks
 * @param estimatedViewRateBp estimated 24h views rate in basis points
 */
@ConfigurationProperties(prefix = "app.marketplace.channel.statistics")
@PropertyGroupDoc(
        displayName = "Channel Statistics Collector",
        description = "Periodic Telegram subscriber sync for channels",
        category = "Marketplace"
)
@Validated
public record ChannelStatisticsCollectorProperties(
        @PropertyDoc(
                description = "Enable periodic channel statistics collection",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("true") boolean enabled,

        @PropertyDoc(
                description = "Maximum number of channels processed per cycle",
                required = Requirement.OPTIONAL
        )
        @Positive @DefaultValue("100") int batchSize,

        @PropertyDoc(
                description = "Backoff in milliseconds between retries"
                        + " for transient Telegram failures",
                required = Requirement.OPTIONAL
        )
        @PositiveOrZero @DefaultValue("1000") long retryBackoffMs,

        @PropertyDoc(
                description = "Maximum retries per channel for transient Telegram failures",
                required = Requirement.OPTIONAL
        )
        @PositiveOrZero @DefaultValue("2") int maxRetriesPerChannel,

        @PropertyDoc(
                description = "Minimum interval between periodic admin list checks",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("24h") Duration adminCheckInterval,

        @PropertyDoc(
                description = "Estimated 24h views ratio in basis points"
                        + " for automatic avg_views/engagement_rate updates",
                required = Requirement.OPTIONAL
        )
        @Min(0) @Max(10000) @DefaultValue("1200") int estimatedViewRateBp
) {
}

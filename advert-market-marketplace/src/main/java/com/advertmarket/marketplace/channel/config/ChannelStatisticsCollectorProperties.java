package com.advertmarket.marketplace.channel.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for channel statistics background collection.
 *
 * @param enabled enables/disables periodic collection
 * @param batchSize maximum channels processed per cycle
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
        @Positive @DefaultValue("100") int batchSize
) {
}

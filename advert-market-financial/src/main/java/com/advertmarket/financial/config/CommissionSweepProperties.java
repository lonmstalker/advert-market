package com.advertmarket.financial.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for commission sweep scheduler.
 */
@ConfigurationProperties(prefix = "app.financial.commission-sweep")
@PropertyGroupDoc(
        displayName = "Commission Sweep",
        description = "Scheduled sweep of accumulated commissions to treasury",
        category = "Financial"
)
public record CommissionSweepProperties(

        @PropertyDoc(
                description = "Cron expression for sweep schedule (default: daily 02:00 UTC)",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("0 0 2 * * *") String cron,

        @PropertyDoc(
                description = "Minimum balance in nanoTON to trigger sweep (dust threshold)",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("1000") long dustThresholdNano,

        @PropertyDoc(
                description = "Max accounts per sweep batch",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("100") int batchSize,

        @PropertyDoc(
                description = "Distributed lock TTL for sweep",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("5m") Duration lockTtl
) {}

package com.advertmarket.financial.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for the ledger service.
 */
@ConfigurationProperties(prefix = "app.ledger")
@PropertyGroupDoc(
        displayName = "Ledger",
        description = "Double-entry bookkeeping configuration",
        category = "Financial"
)
public record LedgerProperties(

        @PropertyDoc(
                description = "Balance cache TTL in Redis",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("5m") Duration cacheTtl,

        @PropertyDoc(
                description = "Default page size for entry queries",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("50") int defaultPageSize
) {}

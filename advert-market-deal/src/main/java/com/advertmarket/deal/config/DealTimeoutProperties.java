package com.advertmarket.deal.config;

import com.advertmarket.shared.model.DealStatus;
import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import java.time.Duration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Timeout configuration for deal states.
 *
 * <p>Each non-terminal status maps to a maximum allowed duration.
 * When a deal stays in a status beyond its timeout, the scheduler
 * transitions it to EXPIRED.
 */
@ConfigurationProperties(prefix = "app.deal.timeout")
@PropertyGroupDoc(
        displayName = "Deal Timeout",
        description = "Deadline durations for deal states and scheduler settings",
        category = "Deal"
)
public record DealTimeoutProperties(

        @PropertyDoc(
                description = "Timeout for OFFER_PENDING status",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("48h") Duration offerPending,

        @PropertyDoc(
                description = "Timeout for NEGOTIATING status",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("72h") Duration negotiating,

        @PropertyDoc(
                description = "Timeout for AWAITING_PAYMENT status",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("24h") Duration awaitingPayment,

        @PropertyDoc(
                description = "Timeout for FUNDED status",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("72h") Duration funded,

        @PropertyDoc(
                description = "Timeout for CREATIVE_APPROVED status",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("72h") Duration creativeApproved,

        @PropertyDoc(
                description = "Timeout for SCHEDULED status",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("168h") Duration scheduled,

        @PropertyDoc(
                description = "Grace period before processing expired deals",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("5m") Duration gracePeriod,

        @PropertyDoc(
                description = "Maximum deals to process per poll cycle",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("50") int batchSize,

        @PropertyDoc(
                description = "Distributed lock TTL for scheduler execution",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("2m") Duration lockTtl
) {

    /**
     * Returns the timeout duration for the given status, or null
     * if the status has no timeout configured.
     */
    public @Nullable Duration timeoutFor(@NonNull DealStatus status) {
        return switch (status) {
            case OFFER_PENDING -> offerPending;
            case NEGOTIATING -> negotiating;
            case AWAITING_PAYMENT -> awaitingPayment;
            case FUNDED -> funded;
            case CREATIVE_APPROVED -> creativeApproved;
            case SCHEDULED -> scheduled;
            default -> null;
        };
    }
}

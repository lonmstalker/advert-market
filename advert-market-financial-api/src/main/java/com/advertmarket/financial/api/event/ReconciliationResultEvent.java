package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Event emitted when a reconciliation process completes.
 *
 * @param triggerId the identifier of the reconciliation trigger
 * @param checks results of each reconciliation check
 * @param completedAt when the reconciliation completed
 */
public record ReconciliationResultEvent(
        @NonNull UUID triggerId,
        @NonNull Map<ReconciliationCheck, ReconciliationCheckResult> checks,
        @NonNull Instant completedAt) implements DomainEvent {

    /**
     * Creates a reconciliation result event with defensive copy.
     *
     * @throws NullPointerException if any parameter is null
     */
    public ReconciliationResultEvent {
        Objects.requireNonNull(triggerId, "triggerId");
        Objects.requireNonNull(checks, "checks");
        Objects.requireNonNull(completedAt, "completedAt");
        checks = Map.copyOf(checks);
    }
}

package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Event to trigger a reconciliation process.
 *
 * @param triggerType how the reconciliation was triggered
 * @param timeRangeStart start of the time range to reconcile
 * @param timeRangeEnd end of the time range to reconcile
 * @param checks list of checks to perform
 */
public record ReconciliationStartEvent(
        @NonNull ReconciliationTriggerType triggerType,
        @NonNull Instant timeRangeStart,
        @NonNull Instant timeRangeEnd,
        @NonNull List<ReconciliationCheck> checks)
        implements DomainEvent {

    /**
     * Creates a reconciliation start event with defensive copy.
     *
     * @throws NullPointerException if any parameter is null
     */
    public ReconciliationStartEvent {
        Objects.requireNonNull(triggerType, "triggerType");
        Objects.requireNonNull(timeRangeStart, "timeRangeStart");
        Objects.requireNonNull(timeRangeEnd, "timeRangeEnd");
        Objects.requireNonNull(checks, "checks");
        checks = List.copyOf(checks);
    }
}

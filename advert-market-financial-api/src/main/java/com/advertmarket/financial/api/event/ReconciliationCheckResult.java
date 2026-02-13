package com.advertmarket.financial.api.event;

import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Result of a single reconciliation check.
 *
 * @param status whether the check passed or failed
 * @param details additional details about the check result
 */
public record ReconciliationCheckResult(
        @NonNull ReconciliationCheckStatus status,
        @NonNull Map<String, Object> details) {

    /**
     * Creates a reconciliation check result with defensive copy.
     *
     * @throws NullPointerException if any parameter is null
     */
    public ReconciliationCheckResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(details, "details");
        details = Map.copyOf(details);
    }
}

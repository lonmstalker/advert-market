package com.advertmarket.financial.api.port;

import com.advertmarket.financial.api.event.ReconciliationResultEvent;
import com.advertmarket.shared.event.EventEnvelope;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for handling reconciliation result events.
 *
 * <p>Separate from {@link FinancialEventPort} because reconciliation
 * uses a different topic, consumer group, retry policy, and is not
 * tied to a specific deal.
 */
public interface ReconciliationResultPort {

    /**
     * Handles a reconciliation result event.
     *
     * @param envelope the event envelope
     */
    void onReconciliationResult(
            @NonNull EventEnvelope<ReconciliationResultEvent> envelope);
}

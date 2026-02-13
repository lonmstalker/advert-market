package com.advertmarket.financial.api.port;

import com.advertmarket.financial.api.event.DepositConfirmedEvent;
import com.advertmarket.financial.api.event.DepositFailedEvent;
import com.advertmarket.financial.api.event.PayoutCompletedEvent;
import com.advertmarket.financial.api.event.RefundCompletedEvent;
import com.advertmarket.shared.event.EventEnvelope;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for handling financial result events from workers.
 */
public interface FinancialEventPort {

    /**
     * Handles a confirmed deposit event.
     *
     * @param envelope the event envelope
     */
    void onDepositConfirmed(
            @NonNull EventEnvelope<DepositConfirmedEvent> envelope);

    /**
     * Handles a failed deposit event.
     *
     * @param envelope the event envelope
     */
    void onDepositFailed(
            @NonNull EventEnvelope<DepositFailedEvent> envelope);

    /**
     * Handles a completed payout event.
     *
     * @param envelope the event envelope
     */
    void onPayoutCompleted(
            @NonNull EventEnvelope<PayoutCompletedEvent> envelope);

    /**
     * Handles a completed refund event.
     *
     * @param envelope the event envelope
     */
    void onRefundCompleted(
            @NonNull EventEnvelope<RefundCompletedEvent> envelope);
}

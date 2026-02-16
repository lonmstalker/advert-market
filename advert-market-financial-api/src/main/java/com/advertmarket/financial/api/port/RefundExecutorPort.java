package com.advertmarket.financial.api.port;

import com.advertmarket.financial.api.event.ExecuteRefundCommand;
import com.advertmarket.shared.event.EventEnvelope;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for executing TON refunds to advertisers.
 */
public interface RefundExecutorPort {

    /**
     * Executes a refund for a cancelled/disputed deal.
     *
     * @param envelope the refund command envelope
     */
    void executeRefund(
            @NonNull EventEnvelope<ExecuteRefundCommand> envelope);
}

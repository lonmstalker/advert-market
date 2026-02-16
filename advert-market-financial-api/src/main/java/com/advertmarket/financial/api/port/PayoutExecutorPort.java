package com.advertmarket.financial.api.port;

import com.advertmarket.financial.api.event.ExecutePayoutCommand;
import com.advertmarket.shared.event.EventEnvelope;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for executing TON payouts to channel owners.
 */
public interface PayoutExecutorPort {

    /**
     * Executes a payout for a completed deal.
     *
     * @param envelope the payout command envelope
     */
    void executePayout(
            @NonNull EventEnvelope<ExecutePayoutCommand> envelope);
}

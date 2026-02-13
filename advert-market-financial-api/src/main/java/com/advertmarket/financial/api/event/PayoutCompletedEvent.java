package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Event emitted when a TON payout is confirmed on chain.
 *
 * @param txHash blockchain transaction hash
 * @param amountNano payout amount in nanoTON
 * @param commissionNano commission amount in nanoTON
 * @param toAddress recipient's TON address
 * @param confirmations number of block confirmations
 */
public record PayoutCompletedEvent(
        @NonNull String txHash,
        long amountNano,
        long commissionNano,
        @NonNull String toAddress,
        int confirmations) implements DomainEvent {
}

package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Event emitted when a TON refund is confirmed on chain.
 *
 * @param txHash blockchain transaction hash
 * @param amountNano refund amount in nanoTON
 * @param toAddress recipient's TON address
 * @param confirmations number of block confirmations
 */
public record RefundCompletedEvent(
        @NonNull String txHash,
        long amountNano,
        @NonNull String toAddress,
        int confirmations) implements DomainEvent {
}

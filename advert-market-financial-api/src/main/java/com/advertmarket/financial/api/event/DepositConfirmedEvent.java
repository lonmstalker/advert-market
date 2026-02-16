package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Event emitted when a TON deposit is confirmed on chain.
 *
 * @param txHash blockchain transaction hash
 * @param amountNano confirmed (received) deposit amount in nanoTON
 * @param expectedAmountNano expected deposit amount in nanoTON
 * @param confirmations number of block confirmations
 * @param fromAddress sender's TON address
 * @param depositAddress platform's deposit address
 */
public record DepositConfirmedEvent(
        @NonNull String txHash,
        long amountNano,
        long expectedAmountNano,
        int confirmations,
        @NonNull String fromAddress,
        @NonNull String depositAddress) implements DomainEvent {
}

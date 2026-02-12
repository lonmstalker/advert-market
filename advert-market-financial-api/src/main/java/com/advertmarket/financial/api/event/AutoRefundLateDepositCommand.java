package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Command to automatically refund a deposit that arrived late.
 *
 * @param txHash blockchain transaction hash
 * @param amountNano deposit amount in nanoTON
 * @param refundAddress TON address to send refund to
 * @param subwalletId TON sub-wallet ID for the transaction
 */
public record AutoRefundLateDepositCommand(
        @NonNull String txHash,
        long amountNano,
        @NonNull String refundAddress,
        int subwalletId) implements DomainEvent {
}

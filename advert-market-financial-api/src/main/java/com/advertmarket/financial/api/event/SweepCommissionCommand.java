package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Command to sweep accumulated commission to treasury.
 *
 * @param commissionAccountId commission account identifier
 * @param amountNano commission amount in nanoTON
 * @param subwalletId TON sub-wallet ID for the transaction
 */
public record SweepCommissionCommand(
        @NonNull String commissionAccountId,
        long amountNano,
        int subwalletId) implements DomainEvent {
}

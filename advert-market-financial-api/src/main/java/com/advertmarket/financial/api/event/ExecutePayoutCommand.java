package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;

/**
 * Command to execute a payout to the channel owner.
 *
 * @param ownerId channel owner's user ID
 * @param amountNano payout amount in nanoTON
 * @param commissionNano platform commission in nanoTON
 * @param subwalletId TON sub-wallet ID for the transaction
 */
public record ExecutePayoutCommand(
        long ownerId,
        long amountNano,
        long commissionNano,
        int subwalletId) implements DomainEvent {
}

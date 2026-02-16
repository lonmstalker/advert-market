package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;

/**
 * Event emitted when a payout is deferred because the
 * channel owner has no TON address registered.
 *
 * @param ownerId channel owner's user ID
 * @param amountNano payout amount in nanoTON
 */
public record PayoutDeferredEvent(
        long ownerId,
        long amountNano) implements DomainEvent {
}

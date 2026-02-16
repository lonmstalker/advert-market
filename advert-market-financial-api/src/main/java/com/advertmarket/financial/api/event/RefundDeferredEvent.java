package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;

/**
 * Event emitted when a refund is deferred because the
 * advertiser has no TON address registered.
 *
 * @param advertiserId advertiser's user ID
 * @param amountNano refund amount in nanoTON
 */
public record RefundDeferredEvent(
        long advertiserId,
        long amountNano) implements DomainEvent {
}

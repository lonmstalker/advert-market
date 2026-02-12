package com.advertmarket.delivery.api.event;

import com.advertmarket.shared.event.DomainEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Event emitted when delivery verification completes.
 *
 * @param messageId Telegram message ID
 * @param checksPassed number of checks that passed
 * @param checksFailed number of checks that failed
 * @param finalContentHash content hash at verification time
 */
public record DeliveryVerifiedEvent(
        long messageId,
        int checksPassed,
        int checksFailed,
        @NonNull String finalContentHash) implements DomainEvent {
}

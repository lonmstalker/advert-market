package com.advertmarket.delivery.api.event;

import com.advertmarket.shared.event.DomainEvent;
import java.time.Instant;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Event emitted when delivery verification detects a failure.
 *
 * @param messageId Telegram message ID
 * @param reason the failure reason
 * @param checkNumber check number when failure was detected
 * @param detectedAt when the failure was detected
 */
public record DeliveryFailedEvent(
        long messageId,
        @NonNull DeliveryFailureReason reason,
        int checkNumber,
        @NonNull Instant detectedAt) implements DomainEvent {
}

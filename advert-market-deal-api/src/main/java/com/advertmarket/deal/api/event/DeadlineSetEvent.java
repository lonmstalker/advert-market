package com.advertmarket.deal.api.event;

import com.advertmarket.shared.event.DomainEvent;
import com.advertmarket.shared.model.DealStatus;
import java.time.Instant;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Event emitted when a deadline is set for a deal.
 *
 * @param expectedStatus the status expected at deadline time
 * @param deadlineAt when the deadline expires
 * @param action what to do when the deadline fires
 * @param refundRequired whether a refund is needed on expiry
 */
public record DeadlineSetEvent(
        @NonNull DealStatus expectedStatus,
        @NonNull Instant deadlineAt,
        @NonNull DeadlineAction action,
        boolean refundRequired) implements DomainEvent {
}

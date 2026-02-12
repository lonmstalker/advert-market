package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Event emitted when a deposit fails.
 *
 * @param reason the failure reason
 * @param expectedNano expected deposit amount in nanoTON
 * @param receivedNano actually received amount in nanoTON
 */
public record DepositFailedEvent(
        @NonNull DepositFailureReason reason,
        long expectedNano,
        long receivedNano) implements DomainEvent {
}

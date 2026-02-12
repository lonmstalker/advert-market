package com.advertmarket.deal.api.event;

import com.advertmarket.shared.event.DomainEvent;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealStatus;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Event emitted when a deal transitions between states.
 *
 * @param fromStatus previous deal status
 * @param toStatus new deal status
 * @param actorId initiating user ID, null for SYSTEM actions
 * @param actorType type of actor who triggered the transition
 * @param dealAmountNano deal amount in nanoTON
 * @param channelId target Telegram channel ID
 */
public record DealStateChangedEvent(
        @NonNull DealStatus fromStatus,
        @NonNull DealStatus toStatus,
        @Nullable Long actorId,
        @NonNull ActorType actorType,
        long dealAmountNano,
        long channelId) implements DomainEvent {
}

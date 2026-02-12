package com.advertmarket.delivery.api.event;

import com.advertmarket.shared.event.DomainEvent;
import java.time.Instant;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Command to publish a post to a Telegram channel.
 *
 * @param channelId target Telegram channel ID
 * @param creativeDraft the creative content to publish
 * @param scheduledAt optional scheduled publication time
 */
public record PublishPostCommand(
        long channelId,
        @NonNull CreativeDraft creativeDraft,
        @Nullable Instant scheduledAt) implements DomainEvent {
}

package com.advertmarket.delivery.api.event;

import com.advertmarket.shared.event.DomainEvent;
import java.time.Instant;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Event emitted when a post publication attempt completes.
 *
 * @param success whether the publication succeeded
 * @param messageId Telegram message ID (0 if failed)
 * @param channelId Telegram channel ID
 * @param contentHash hash of the published content (null if failed)
 * @param publishedAt when the post was published (null if failed)
 * @param error error message (null if succeeded)
 * @param details additional error details (null if succeeded)
 */
public record PublicationResultEvent(
        boolean success,
        long messageId,
        long channelId,
        @Nullable String contentHash,
        @Nullable Instant publishedAt,
        @Nullable String error,
        @Nullable String details) implements DomainEvent {
}

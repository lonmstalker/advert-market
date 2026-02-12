package com.advertmarket.delivery.api.event;

import com.advertmarket.shared.event.DomainEvent;
import java.time.Instant;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Command to verify that a published post is still live.
 *
 * @param channelId Telegram channel ID
 * @param messageId Telegram message ID of the post
 * @param contentHash expected content hash of the post
 * @param publishedAt when the post was originally published
 * @param checkNumber sequential verification check number
 */
public record VerifyDeliveryCommand(
        long channelId,
        long messageId,
        @NonNull String contentHash,
        @NonNull Instant publishedAt,
        int checkNumber) implements DomainEvent {
}

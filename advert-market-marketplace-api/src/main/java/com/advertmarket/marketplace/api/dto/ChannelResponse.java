package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Registered channel representation.
 *
 * @param id               channel (Telegram chat) identifier
 * @param title            channel title
 * @param username         public username
 * @param description      channel description
 * @param subscriberCount  number of subscribers
 * @param category         channel category
 * @param pricePerPostNano price per post in nanoTON
 * @param isActive         whether the channel is active
 * @param ownerId          owner user identifier
 * @param createdAt        registration timestamp
 */
@Schema(description = "Registered channel")
public record ChannelResponse(
        long id,
        @NonNull String title,
        @Nullable String username,
        @Nullable String description,
        int subscriberCount,
        @Nullable String category,
        @Nullable Long pricePerPostNano,
        boolean isActive,
        long ownerId,
        @NonNull OffsetDateTime createdAt
) {
}

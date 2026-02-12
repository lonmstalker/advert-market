package com.advertmarket.marketplace.api.dto;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Data required to insert a new channel.
 *
 * @param telegramId      Telegram chat identifier
 * @param title           channel title
 * @param username        public username
 * @param description     channel description
 * @param subscriberCount subscriber count
 * @param category        optional category
 * @param pricePerPostNano optional price in nanoTON
 * @param ownerId         owner user identifier
 */
public record NewChannel(
        long telegramId,
        @NonNull String title,
        @Nullable String username,
        @Nullable String description,
        int subscriberCount,
        @Nullable String category,
        @Nullable Long pricePerPostNano,
        long ownerId
) {
}

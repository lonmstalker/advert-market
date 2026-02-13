package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Data required to insert a new channel.
 *
 * @param telegramId       Telegram chat identifier
 * @param title            channel title
 * @param username         public username
 * @param description      channel description
 * @param subscriberCount  subscriber count
 * @param categories       category slugs
 * @param pricePerPostNano optional price in nanoTON
 * @param ownerId          owner user identifier
 */
@Schema(description = "New channel data for insertion")
public record NewChannel(
        long telegramId,
        @NonNull String title,
        @Nullable String username,
        @Nullable String description,
        int subscriberCount,
        @NonNull List<String> categories,
        @Nullable Long pricePerPostNano,
        long ownerId
) {

    /** Defensively copies categories. */
    public NewChannel {
        categories = List.copyOf(categories);
    }
}

package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Request to register a verified Telegram channel.
 *
 * @param channelId        Telegram chat identifier (from verify step)
 * @param categories       optional category slugs
 * @param pricePerPostNano optional price per post in nanoTON
 */
@Schema(description = "Channel registration request")
public record ChannelRegistrationRequest(
        @Schema(description = "Telegram chat ID from verify step")
        long channelId,

        @Schema(description = "Category slugs", example = "[\"tech\"]")
        @Nullable List<String> categories,

        @Schema(description = "Price per post in nanoTON")
        @Nullable Long pricePerPostNano
) {
}

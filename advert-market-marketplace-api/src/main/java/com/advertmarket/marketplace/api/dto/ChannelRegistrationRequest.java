package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Request to register a verified Telegram channel.
 *
 * @param channelId        Telegram chat identifier (from verify step)
 * @param category         optional channel category
 * @param pricePerPostNano optional price per post in nanoTON
 */
@Schema(description = "Channel registration request")
public record ChannelRegistrationRequest(
        @Schema(description = "Telegram chat ID from verify step")
        long channelId,

        @Schema(description = "Channel category", example = "tech")
        @Nullable @Size(max = 100) String category,

        @Schema(description = "Price per post in nanoTON")
        @Nullable Long pricePerPostNano
) {
}

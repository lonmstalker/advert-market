package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Lightweight projection of a channel with its owner.
 *
 * @param channelId Telegram channel id (channels.id)
 * @param ownerId   Telegram user id of the channel owner (channels.owner_id)
 * @param title     channel title
 */
@Schema(description = "Channel owner projection")
public record ChannelOwnerInfo(
        long channelId,
        long ownerId,
        @NonNull String title
) {
}
package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Result of channel synchronization from Telegram state.
 *
 * @param ownerChanged whether channel owner changed during sync
 * @param oldOwnerId previous owner id, if channel existed before sync
 * @param newOwnerId owner id resolved from Telegram creator
 */
@Schema(description = "Channel auto-sync result")
public record ChannelSyncResult(
        boolean ownerChanged,
        @Nullable Long oldOwnerId,
        long newOwnerId
) {
}

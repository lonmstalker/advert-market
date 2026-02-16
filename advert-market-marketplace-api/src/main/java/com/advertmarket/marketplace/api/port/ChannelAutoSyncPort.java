package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.dto.ChannelSyncResult;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for live channel synchronization with Telegram state.
 */
public interface ChannelAutoSyncPort {

    /**
     * Synchronizes channel owner/admin state from Telegram.
     *
     * @param channelId Telegram channel id
     * @return sync result including owner transfer information
     */
    @NonNull
    ChannelSyncResult syncFromTelegram(long channelId);
}

package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.dto.NewChannel;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Repository port for channel persistence.
 */
public interface ChannelRepository {

    /** Returns {@code true} if a channel with this Telegram ID exists. */
    boolean existsByTelegramId(long telegramId);

    /**
     * Inserts a new channel and its OWNER membership atomically.
     *
     * @param newChannel channel data to persist
     * @return the persisted channel
     */
    @NonNull
    ChannelResponse insert(@NonNull NewChannel newChannel);

    /** Finds a channel by its Telegram ID. */
    @NonNull
    Optional<ChannelResponse> findByTelegramId(long telegramId);

    /** Finds a channel with full detail including pricing rules. */
    @NonNull
    Optional<ChannelDetailResponse> findDetailById(long channelId);

    /**
     * Updates channel fields. Uses optimistic locking via version.
     *
     * @param channelId channel to update
     * @param request   fields to update
     * @return updated channel, or empty if not found
     */
    @NonNull
    Optional<ChannelResponse> update(long channelId,
                                     @NonNull ChannelUpdateRequest request);

    /** Deactivates a channel (sets is_active = false). */
    boolean deactivate(long channelId);
}

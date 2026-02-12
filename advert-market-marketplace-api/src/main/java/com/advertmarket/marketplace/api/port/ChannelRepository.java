package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.dto.ChannelResponse;
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
}

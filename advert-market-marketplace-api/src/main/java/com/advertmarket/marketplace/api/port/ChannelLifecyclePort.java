package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.dto.ChannelOwnerInfo;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for channel lifecycle operations triggered by external events
 * (e.g. bot membership changes).
 *
 * <p>Implementations live in the marketplace module and use
 * optimistic locking via the {@code version} column.
 */
public interface ChannelLifecyclePort {

    /**
     * Deactivates the channel by its Telegram id.
     *
     * @param telegramChannelId Telegram channel id
     * @return true if the channel was found and deactivated
     */
    boolean deactivateByTelegramId(long telegramChannelId);

    /**
     * Reactivates the channel by its Telegram id.
     *
     * @param telegramChannelId Telegram channel id
     * @return true if the channel was found and reactivated
     */
    boolean reactivateByTelegramId(long telegramChannelId);

    /**
     * Returns channel owner info by Telegram channel id.
     *
     * @param telegramChannelId Telegram channel id
     * @return owner info or empty if the channel is not registered
     */
    @NonNull
    Optional<ChannelOwnerInfo> findOwnerByTelegramId(long telegramChannelId);
}
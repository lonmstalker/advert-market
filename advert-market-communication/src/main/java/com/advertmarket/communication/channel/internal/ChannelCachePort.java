package com.advertmarket.communication.channel.internal;

import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for caching Telegram channel data.
 */
public interface ChannelCachePort {

    /**
     * Gets cached chat info.
     *
     * @param channelId the channel identifier
     * @return cached chat info, or empty if not cached
     */
    @NonNull
    Optional<ChatInfo> getChatInfo(long channelId);

    /**
     * Stores chat info in cache.
     *
     * @param channelId the channel identifier
     * @param chatInfo  the chat info to cache
     */
    void putChatInfo(long channelId, @NonNull ChatInfo chatInfo);

    /**
     * Gets cached administrator list.
     *
     * @param channelId the channel identifier
     * @return cached admins, or empty if not cached
     */
    @NonNull
    Optional<List<ChatMemberInfo>> getAdministrators(long channelId);

    /**
     * Stores administrator list in cache.
     *
     * @param channelId the channel identifier
     * @param admins    the admin list to cache
     */
    void putAdministrators(long channelId,
            @NonNull List<ChatMemberInfo> admins);

    /**
     * Evicts all cached data for a channel.
     *
     * @param channelId the channel identifier
     */
    void evict(long channelId);
}

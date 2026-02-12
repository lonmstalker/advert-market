package com.advertmarket.communication.api.channel;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for querying Telegram channel information via Bot API.
 *
 * <p>Provides cached access to channel metadata, member info,
 * and administrator lists. Implementations handle rate limiting,
 * caching and circuit breaker fallback.
 */
public interface TelegramChannelPort {

    /**
     * Retrieves chat information for a Telegram channel.
     *
     * @param channelId the Telegram chat identifier
     * @return chat info
     */
    @NonNull
    ChatInfo getChat(long channelId);

    /**
     * Retrieves membership info for a specific user in a channel.
     *
     * @param channelId the Telegram chat identifier
     * @param userId    the Telegram user identifier
     * @return member info
     */
    @NonNull
    ChatMemberInfo getChatMember(long channelId, long userId);

    /**
     * Retrieves the list of administrators for a channel.
     *
     * @param channelId the Telegram chat identifier
     * @return list of admin member infos
     */
    @NonNull
    List<ChatMemberInfo> getChatAdministrators(long channelId);

    /**
     * Retrieves the total number of members in a channel.
     *
     * @param channelId the Telegram chat identifier
     * @return member count
     */
    int getChatMemberCount(long channelId);
}

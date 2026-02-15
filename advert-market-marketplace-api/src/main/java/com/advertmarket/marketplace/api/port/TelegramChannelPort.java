package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for querying Telegram channel information via Bot API.
 *
 * <p>Implemented in {@code communication} module (Telegram Bot API integration),
 * consumed by {@code marketplace} for channel verification.
 */
public interface TelegramChannelPort {

    /** Retrieves chat information for a Telegram channel. */
    @NonNull
    ChatInfo getChat(long channelId);

    /** Retrieves chat information by public username (e.g. "mychannel"). */
    @NonNull
    ChatInfo getChatByUsername(@NonNull String username);

    /** Retrieves membership info for a specific user in a channel. */
    @NonNull
    ChatMemberInfo getChatMember(long channelId, long userId);

    /** Retrieves the list of administrators for a channel. */
    @NonNull
    List<ChatMemberInfo> getChatAdministrators(long channelId);

    /** Retrieves the total number of members in a channel. */
    int getChatMemberCount(long channelId);
}

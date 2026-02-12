package com.advertmarket.communication.api.channel;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Information about a member of a Telegram chat.
 *
 * @param userId            Telegram user identifier
 * @param status            member status in the chat
 * @param canPostMessages   can post messages in the channel
 * @param canEditMessages   can edit messages of other users
 * @param canDeleteMessages can delete messages of other users
 * @param canManageChat     can manage the chat (change info, pin messages, etc.)
 */
public record ChatMemberInfo(
        long userId,
        @NonNull ChatMemberStatus status,
        boolean canPostMessages,
        boolean canEditMessages,
        boolean canDeleteMessages,
        boolean canManageChat
) {
}

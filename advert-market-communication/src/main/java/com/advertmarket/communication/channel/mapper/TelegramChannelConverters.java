package com.advertmarket.communication.channel.mapper;

import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberStatus;
import com.pengrad.telegrambot.model.ChatFullInfo;
import com.pengrad.telegrambot.model.ChatMember;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Component;

/**
 * Converters from Telegram SDK models to internal API models.
 */
@Component
public class TelegramChannelConverters {

    /**
     * Converts Telegram {@link ChatFullInfo} to internal {@link ChatInfo}.
     */
    @NonNull
    public ChatInfo toChatInfo(@NonNull ChatFullInfo chat) {
        return new ChatInfo(
                chat.id(),
                chat.title() != null ? chat.title() : "",
                chat.username(),
                chat.type() != null ? chat.type().name() : "unknown",
                chat.description());
    }

    /**
     * Converts Telegram {@link ChatMember} to internal {@link ChatMemberInfo}.
     */
    @NonNull
    public ChatMemberInfo toChatMemberInfo(@NonNull ChatMember cm) {
        long userId = cm.user().id();
        return switch (cm.status()) {
            case creator -> new ChatMemberInfo(userId,
                    ChatMemberStatus.CREATOR,
                    true, true, true, true);
            case administrator -> new ChatMemberInfo(userId,
                    ChatMemberStatus.ADMINISTRATOR,
                    cm.canPostMessages(),
                    cm.canEditMessages(),
                    cm.canDeleteMessages(),
                    cm.canManageChat());
            case member -> new ChatMemberInfo(userId,
                    ChatMemberStatus.MEMBER,
                    false, false, false, false);
            case restricted -> new ChatMemberInfo(userId,
                    ChatMemberStatus.RESTRICTED,
                    false, false, false, false);
            case left -> new ChatMemberInfo(userId,
                    ChatMemberStatus.LEFT,
                    false, false, false, false);
            case kicked -> new ChatMemberInfo(userId,
                    ChatMemberStatus.KICKED,
                    false, false, false, false);
        };
    }
}

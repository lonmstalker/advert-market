package com.advertmarket.marketplace.channel.mapper;

import com.advertmarket.communication.api.channel.ChatInfo;
import com.advertmarket.communication.api.channel.ChatMemberInfo;
import com.advertmarket.communication.api.channel.ChatMemberStatus;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse.BotStatus;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse.UserStatus;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Factory for {@link ChannelVerifyResponse} and nested status DTOs.
 *
 * <p>Kept outside {@code ..service..} to avoid manual DTO construction in services.
 */
public final class ChannelVerifyResponseFactory {

    private ChannelVerifyResponseFactory() {
    }

    /**
     * Builds {@link ChannelVerifyResponse} from resolved Telegram channel state.
     */
    @NonNull
    public static ChannelVerifyResponse toResponse(
            @NonNull ChatInfo chatInfo,
            int subscriberCount,
            @NonNull ChatMemberInfo botMember,
            @NonNull ChatMemberInfo userMember) {
        return new ChannelVerifyResponse(
                chatInfo.id(),
                chatInfo.title(),
                chatInfo.username(),
                subscriberCount,
                toBotStatus(botMember),
                toUserStatus(userMember));
    }

    @NonNull
    static BotStatus toBotStatus(@NonNull ChatMemberInfo bot) {
        boolean isAdmin = bot.status() == ChatMemberStatus.CREATOR
                || bot.status() == ChatMemberStatus.ADMINISTRATOR;
        List<String> missing = new ArrayList<>();
        if (!bot.canPostMessages()) {
            missing.add("can_post_messages");
        }
        if (!bot.canEditMessages()) {
            missing.add("can_edit_messages");
        }
        return new BotStatus(
                isAdmin,
                bot.canPostMessages(),
                bot.canEditMessages(),
                List.copyOf(missing));
    }

    @NonNull
    static UserStatus toUserStatus(@NonNull ChatMemberInfo user) {
        boolean isMember = user.status() != ChatMemberStatus.LEFT
                && user.status() != ChatMemberStatus.KICKED;
        return new UserStatus(isMember, user.status().name());
    }
}

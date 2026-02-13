package com.advertmarket.marketplace.channel.service;

import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_BOT_INSUFFICIENT_RIGHTS;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_BOT_NOT_ADMIN;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_BOT_NOT_MEMBER;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_NOT_FOUND;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_USER_NOT_ADMIN;

import com.advertmarket.communication.api.channel.ChatInfo;
import com.advertmarket.communication.api.channel.ChatMemberInfo;
import com.advertmarket.communication.api.channel.ChatMemberStatus;
import com.advertmarket.communication.api.channel.TelegramChannelPort;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse.BotStatus;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse.UserStatus;
import com.advertmarket.marketplace.channel.config.ChannelBotProperties;
import com.advertmarket.shared.exception.DomainException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Verifies that a Telegram channel is suitable for registration:
 * the bot must be an admin with required permissions,
 * and the requesting user must be a channel admin/creator.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(ChannelBotProperties.class)
public class ChannelVerificationService {

    private static final String TYPE_CHANNEL = "channel";

    private final TelegramChannelPort telegramChannel;
    private final ChannelBotProperties botProperties;

    /**
     * Resolves a channel by username and verifies bot + user status.
     *
     * @param username channel username (without @)
     * @param userId   Telegram user ID of the requester
     * @return verification result
     */
    @NonNull
    public ChannelVerifyResponse verify(@NonNull String username,
                                        long userId) {
        var chatInfo = resolveChannel(username);
        return verifyBotAndUser(chatInfo, userId);
    }

    /**
     * Resolves a channel by its numeric Telegram ID.
     * Used for re-verification during registration.
     */
    @NonNull
    ChatInfo resolveChannelById(long channelId) {
        var chatInfo = telegramChannel.getChat(channelId);
        if (!TYPE_CHANNEL.equalsIgnoreCase(chatInfo.type())) {
            throw new DomainException(CHANNEL_NOT_FOUND,
                    "Chat " + channelId + " is not a channel, "
                            + "type: " + chatInfo.type());
        }
        return chatInfo;
    }

    @NonNull
    ChatInfo resolveChannel(@NonNull String username) {
        var chatInfo = telegramChannel.getChatByUsername(username);
        if (!TYPE_CHANNEL.equalsIgnoreCase(chatInfo.type())) {
            throw new DomainException(CHANNEL_NOT_FOUND,
                    "Chat @" + username + " is not a channel, "
                            + "type: " + chatInfo.type());
        }
        return chatInfo;
    }

    @NonNull
    ChannelVerifyResponse verifyBotAndUser(@NonNull ChatInfo chatInfo,
                                           long userId) {
        long channelId = chatInfo.id();
        long botId = botProperties.botUserId();

        var botMemberFuture = CompletableFuture.supplyAsync(
                () -> telegramChannel.getChatMember(channelId, botId));
        var userMemberFuture = CompletableFuture.supplyAsync(
                () -> telegramChannel.getChatMember(channelId, userId));
        var memberCountFuture = CompletableFuture.supplyAsync(
                () -> telegramChannel.getChatMemberCount(channelId));

        CompletableFuture.allOf(
                botMemberFuture, userMemberFuture, memberCountFuture
        ).join();

        var botMember = botMemberFuture.join();
        var userMember = userMemberFuture.join();
        int memberCount = memberCountFuture.join();

        var botStatus = buildBotStatus(botMember);
        var userStatus = buildUserStatus(userMember);

        validateBot(botMember);
        validateUser(userMember);

        return new ChannelVerifyResponse(
                channelId,
                chatInfo.title(),
                chatInfo.username(),
                memberCount,
                botStatus,
                userStatus);
    }

    private static BotStatus buildBotStatus(
            @NonNull ChatMemberInfo bot) {
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
                isAdmin, bot.canPostMessages(),
                bot.canEditMessages(), List.copyOf(missing));
    }

    private static UserStatus buildUserStatus(
            @NonNull ChatMemberInfo user) {
        boolean isMember = user.status() != ChatMemberStatus.LEFT
                && user.status() != ChatMemberStatus.KICKED;
        return new UserStatus(isMember, user.status().name());
    }

    private static void validateBot(@NonNull ChatMemberInfo bot) {
        if (bot.status() == ChatMemberStatus.LEFT
                || bot.status() == ChatMemberStatus.KICKED) {
            throw new DomainException(CHANNEL_BOT_NOT_MEMBER,
                    "Bot is not a member of the channel");
        }
        if (bot.status() != ChatMemberStatus.CREATOR
                && bot.status() != ChatMemberStatus.ADMINISTRATOR) {
            throw new DomainException(CHANNEL_BOT_NOT_ADMIN,
                    "Bot is not an admin of the channel");
        }
        if (!bot.canPostMessages() || !bot.canEditMessages()) {
            throw new DomainException(CHANNEL_BOT_INSUFFICIENT_RIGHTS,
                    "Bot lacks required permissions");
        }
    }

    private static void validateUser(@NonNull ChatMemberInfo user) {
        if (user.status() != ChatMemberStatus.CREATOR
                && user.status() != ChatMemberStatus.ADMINISTRATOR) {
            throw new DomainException(CHANNEL_USER_NOT_ADMIN,
                    "User is not an admin of the channel");
        }
    }
}

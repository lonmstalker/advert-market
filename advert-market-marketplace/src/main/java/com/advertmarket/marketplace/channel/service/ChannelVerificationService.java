package com.advertmarket.marketplace.channel.service;

import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_BOT_INSUFFICIENT_RIGHTS;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_BOT_NOT_ADMIN;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_NOT_FOUND;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_USER_NOT_ADMIN;

import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberStatus;
import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.marketplace.channel.config.ChannelBotProperties;
import com.advertmarket.marketplace.channel.mapper.ChannelVerifyResponseFactory;
import com.advertmarket.shared.error.ErrorCode;
import com.advertmarket.shared.exception.DomainException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
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
    private static final int RATE_LIMIT_MAX_ATTEMPTS = 2;
    private static final long RATE_LIMIT_RETRY_DELAY_MS = 1100L;

    private final TelegramChannelPort telegramChannel;
    private final ChannelBotProperties botProperties;

    private final @NonNull Executor blockingIoExecutor;

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

        long timeoutMs = botProperties.verificationTimeout().toMillis();
        var admins = callTelegramWithRateLimitRetry(
                channelId,
                timeoutMs,
                () -> telegramChannel.getChatAdministrators(channelId),
                "getChatAdministrators");
        int memberCount = callTelegramWithRateLimitRetry(
                channelId,
                timeoutMs,
                () -> telegramChannel.getChatMemberCount(channelId),
                "getChatMemberCount");
        var botMember = findAdminByUserId(admins, botId)
                .orElseThrow(() -> new DomainException(
                        CHANNEL_BOT_NOT_ADMIN,
                        "Bot is not an admin of the channel"));
        var userMember = findAdminByUserId(admins, userId)
                .orElseThrow(() -> new DomainException(
                        CHANNEL_USER_NOT_ADMIN,
                        "User is not an admin of the channel"));

        validateBot(botMember);
        validateUser(userMember);

        return ChannelVerifyResponseFactory.toResponse(
                chatInfo,
                memberCount,
                botMember,
                userMember);
    }

    private <T> T callTelegramWithRateLimitRetry(
            long channelId,
            long timeoutMs,
            Supplier<T> operation,
            String operationName) {
        for (int attempt = 1;
                attempt <= RATE_LIMIT_MAX_ATTEMPTS;
                attempt++) {
            try {
                return callTelegramWithTimeout(
                        channelId, timeoutMs, operation);
            } catch (DomainException e) {
                ErrorCode resolved = ErrorCode.resolve(
                        e.getErrorCode());
                if (resolved != ErrorCode.RATE_LIMIT_EXCEEDED
                        || attempt == RATE_LIMIT_MAX_ATTEMPTS) {
                    throw e;
                }
                log.warn("Channel={} {} rate-limited, retrying in {} ms",
                        channelId, operationName,
                        RATE_LIMIT_RETRY_DELAY_MS);
                sleepBeforeRateLimitRetry(channelId);
            }
        }
        throw new IllegalStateException("Rate limit retry exhausted");
    }

    private <T> T callTelegramWithTimeout(
            long channelId,
            long timeoutMs,
            Supplier<T> operation) {
        var future = CompletableFuture.supplyAsync(
                operation, blockingIoExecutor).orTimeout(
                timeoutMs, TimeUnit.MILLISECONDS);
        try {
            return future.join();
        } catch (CompletionException e) {
            throw unwrapAsyncFailure(channelId, e);
        }
    }

    private static void sleepBeforeRateLimitRetry(long channelId) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(
                RATE_LIMIT_RETRY_DELAY_MS));
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new DomainException(
                    com.advertmarket.shared.exception.ErrorCodes.SERVICE_UNAVAILABLE,
                    "Interrupted while waiting to retry channel "
                            + channelId,
                    new InterruptedException(
                            "Interrupted while waiting for rate-limit "
                                    + "retry"));
        }
    }

    private static DomainException unwrapAsyncFailure(
            long channelId, Throwable error) {
        Throwable cause = error;
        while (cause instanceof CompletionException) {
            Throwable next = cause.getCause();
            if (next == null) {
                break;
            }
            cause = next;
        }
        if (cause instanceof DomainException domainException) {
            return domainException;
        }
        if (cause instanceof TimeoutException) {
            return new DomainException(
                    com.advertmarket.shared.exception.ErrorCodes.SERVICE_UNAVAILABLE,
                    "Telegram API timeout while verifying channel "
                            + channelId,
                    cause);
        }
        return new DomainException(
                com.advertmarket.shared.exception.ErrorCodes.SERVICE_UNAVAILABLE,
                "Telegram API error while verifying channel "
                        + channelId,
                cause);
    }

    private static java.util.Optional<ChatMemberInfo> findAdminByUserId(
            List<ChatMemberInfo> admins, long userId) {
        return admins.stream()
                .filter(admin -> admin.userId() == userId)
                .findFirst();
    }

    private static void validateBot(@NonNull ChatMemberInfo bot) {
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

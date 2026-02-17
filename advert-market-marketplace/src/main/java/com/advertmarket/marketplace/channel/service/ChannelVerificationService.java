package com.advertmarket.marketplace.channel.service;

import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_BOT_INSUFFICIENT_RIGHTS;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_BOT_NOT_ADMIN;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_NOT_FOUND;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_USER_NOT_ADMIN;
import static com.advertmarket.shared.exception.ErrorCodes.INVALID_PARAMETER;

import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberStatus;
import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.marketplace.channel.config.ChannelBotProperties;
import com.advertmarket.marketplace.channel.mapper.ChannelVerifyResponseFactory;
import com.advertmarket.shared.error.ErrorCode;
import com.advertmarket.shared.exception.DomainException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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
    private static final String T_ME_HOST = "t.me";
    private static final String TELEGRAM_ME_HOST = "telegram.me";
    private static final String JOINCHAT_LINK_PREFIX = "joinchat/";
    private static final String INVITE_HASH_PREFIX = "+";
    private static final long TELEGRAM_CHANNEL_ID_OFFSET = 1_000_000_000_000L;
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
            "^-?\\d+$");
    private static final Pattern PRIVATE_POST_LINK_PATTERN = Pattern.compile(
            "^c/(\\d+)(?:/.*)?$");
    private static final int RATE_LIMIT_MAX_ATTEMPTS = 2;
    private static final long RATE_LIMIT_RETRY_DELAY_MS = 1100L;

    private final TelegramChannelPort telegramChannel;
    private final ChannelBotProperties botProperties;

    private final @NonNull Executor blockingIoExecutor;

    /**
     * Resolves a channel by username and verifies bot + user status.
     *
     * @param reference channel username, numeric id, or private post link
     * @param userId   Telegram user ID of the requester
     * @return verification result
     */
    @NonNull
    public ChannelVerifyResponse verify(@NonNull String reference,
                                        long userId) {
        var chatInfo = resolveChannel(reference);
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
    ChatInfo resolveChannel(@NonNull String reference) {
        String normalized = normalizeChannelReference(reference);
        if (isInviteLink(normalized)) {
            throw invalidParameter("Invite links are not supported for "
                    + "verification. Provide @username, t.me/c/<id>/<post>, "
                    + "or channel id.");
        }

        Long channelId = parseChannelIdOrNull(normalized);
        if (channelId != null) {
            return resolveChannelById(channelId);
        }

        String username = normalizeUsername(normalized);
        if (username.isBlank()) {
            throw invalidParameter("Channel reference is empty");
        }
        var chatInfo = telegramChannel.getChatByUsername(username);
        if (!TYPE_CHANNEL.equalsIgnoreCase(chatInfo.type())) {
            throw new DomainException(CHANNEL_NOT_FOUND,
                    "Chat @" + username + " is not a channel, "
                            + "type: " + chatInfo.type());
        }
        return chatInfo;
    }

    private static boolean isInviteLink(String normalizedReference) {
        return normalizedReference.startsWith(INVITE_HASH_PREFIX)
                || normalizedReference.startsWith(JOINCHAT_LINK_PREFIX);
    }

    @Nullable
    private static Long parseChannelIdOrNull(String normalizedReference) {
        var matcher = PRIVATE_POST_LINK_PATTERN.matcher(normalizedReference);
        if (matcher.matches()) {
            return toChannelIdFromInternal(
                    parsePositiveLong(matcher.group(1),
                            "Invalid private post link"));
        }
        if (normalizedReference.startsWith("c/")) {
            throw invalidParameter("Invalid private post link: "
                    + normalizedReference);
        }

        if (!NUMERIC_PATTERN.matcher(normalizedReference).matches()) {
            return null;
        }
        long parsed = parseLong(normalizedReference, "Invalid channel id");
        return normalizeChannelId(parsed);
    }

    private static long normalizeChannelId(long parsed) {
        if (parsed == 0) {
            throw invalidParameter("Channel id cannot be 0");
        }
        if (parsed <= -TELEGRAM_CHANNEL_ID_OFFSET) {
            return parsed;
        }
        if (parsed < 0) {
            throw invalidParameter("Unsupported channel id format: " + parsed);
        }
        if (parsed >= TELEGRAM_CHANNEL_ID_OFFSET) {
            return -parsed;
        }
        return toChannelIdFromInternal(parsed);
    }

    private static long toChannelIdFromInternal(long internalId) {
        return -(TELEGRAM_CHANNEL_ID_OFFSET + internalId);
    }

    private static String normalizeChannelReference(@NonNull String reference) {
        String trimmed = reference.trim();
        if (trimmed.isBlank()) {
            throw invalidParameter("Channel reference is required");
        }

        String noQuery = stripQueryAndFragment(trimmed);
        String path = extractTelegramPath(noQuery);
        String normalized = trimSlashes(path);

        if (normalized.startsWith("@")) {
            return normalized.substring(1);
        }
        return normalized;
    }

    private static String stripQueryAndFragment(String value) {
        int queryPos = value.indexOf('?');
        int fragmentPos = value.indexOf('#');
        int cutPos;
        if (queryPos < 0) {
            cutPos = fragmentPos;
        } else if (fragmentPos < 0) {
            cutPos = queryPos;
        } else {
            cutPos = Math.min(queryPos, fragmentPos);
        }
        return cutPos >= 0 ? value.substring(0, cutPos) : value;
    }

    private static String extractTelegramPath(String raw) {
        String withScheme = addHttpsToTelegramShortcut(raw);
        if (!hasHttpScheme(withScheme)) {
            return raw;
        }

        URI uri = URI.create(withScheme);
        if (!isTelegramHost(uri.getHost())) {
            return raw;
        }
        return requireTelegramPath(uri.getPath());
    }

    private static String addHttpsToTelegramShortcut(String raw) {
        if (hasHttpScheme(raw) || !isTelegramShortcut(raw)) {
            return raw;
        }
        return "https://" + raw;
    }

    private static boolean hasHttpScheme(String value) {
        return value.startsWith("http://")
                || value.startsWith("https://");
    }

    private static boolean isTelegramShortcut(String value) {
        return value.startsWith("t.me/")
                || value.startsWith("telegram.me/");
    }

    private static boolean isTelegramHost(@Nullable String host) {
        if (host == null) {
            return false;
        }
        return host.equalsIgnoreCase(T_ME_HOST)
                || host.equalsIgnoreCase(TELEGRAM_ME_HOST);
    }

    private static String requireTelegramPath(@Nullable String path) {
        if (path == null || path.isBlank()) {
            throw invalidParameter("Telegram link does not contain channel "
                    + "reference");
        }
        return path;
    }

    private static String trimSlashes(String value) {
        String normalized = value;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0,
                    normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeUsername(String normalizedReference) {
        int slashIndex = normalizedReference.indexOf('/');
        String firstSegment = slashIndex >= 0
                ? normalizedReference.substring(0, slashIndex)
                : normalizedReference;
        return firstSegment.trim();
    }

    private static long parsePositiveLong(String value, String message) {
        long parsed = parseLong(value, message);
        if (parsed <= 0) {
            throw invalidParameter(message + ": " + value);
        }
        return parsed;
    }

    private static long parseLong(String value, String message) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw invalidParameter(message + ": " + value);
        }
    }

    private static DomainException invalidParameter(String message) {
        return new DomainException(INVALID_PARAMETER, message);
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

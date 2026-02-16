package com.advertmarket.communication.channel;

import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_BOT_NOT_MEMBER;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_NOT_FOUND;
import static com.advertmarket.shared.exception.ErrorCodes.RATE_LIMIT_EXCEEDED;
import static com.advertmarket.shared.exception.ErrorCodes.SERVICE_UNAVAILABLE;
import static com.advertmarket.shared.metric.MetricNames.CHANNEL_API_CALL;
import static com.advertmarket.shared.metric.MetricNames.CHANNEL_CACHE_HIT;
import static com.advertmarket.shared.metric.MetricNames.CHANNEL_CACHE_MISS;
import static com.advertmarket.shared.metric.MetricNames.CHANNEL_CACHE_STALE;

import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.communication.channel.internal.ChannelCachePort;
import com.advertmarket.communication.channel.internal.ChannelCacheProperties;
import com.advertmarket.communication.channel.internal.ChannelRateLimiterPort;
import com.advertmarket.communication.channel.mapper.TelegramChannelConverters;
import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.request.GetChat;
import com.pengrad.telegrambot.request.GetChatAdministrators;
import com.pengrad.telegrambot.request.GetChatMember;
import com.pengrad.telegrambot.request.GetChatMemberCount;
import com.pengrad.telegrambot.response.BaseResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link TelegramChannelPort} using the Telegram Bot API.
 *
 * <p>Combines caching, per-channel rate limiting, and circuit breaker
 * fallback via {@link TelegramSender}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(ChannelCacheProperties.class)
public class TelegramChannelService implements TelegramChannelPort {

    private final TelegramSender sender;
    private final ChannelCachePort cache;
    private final ChannelRateLimiterPort rateLimiter;
    private final MetricsFacade metrics;
    private final TelegramChannelConverters converters;

    @Override
    @NonNull
    public ChatInfo getChat(long channelId) {
        var cached = cache.getChatInfo(channelId);
        if (cached.isPresent()) {
            metrics.incrementCounter(CHANNEL_CACHE_HIT,
                    "method", "getChat");
            return cached.get();
        }
        metrics.incrementCounter(CHANNEL_CACHE_MISS,
                "method", "getChat");

        acquireOrThrow(channelId);

        try {
            var response = sender.execute(new GetChat(channelId));
            checkResponse(response, channelId);
            var chatInfo = converters.toChatInfo(response.chat());
            cache.putChatInfo(channelId, chatInfo);
            metrics.incrementCounter(CHANNEL_API_CALL,
                    "method", "getChat", "ok", "true");
            return chatInfo;
        } catch (CallNotPermittedException e) {
            return fallbackChatInfo(channelId);
        }
    }

    @Override
    @NonNull
    public ChatInfo getChatByUsername(@NonNull String username) {
        var chatId = "@" + username;
        try {
            var response = sender.execute(new GetChat(chatId));
            if (!response.isOk()) {
                mapTelegramError(response.errorCode(),
                        response.description(), 0);
            }
            var chatInfo = converters.toChatInfo(response.chat());
            cache.putChatInfo(chatInfo.id(), chatInfo);
            metrics.incrementCounter(CHANNEL_API_CALL,
                    "method", "getChatByUsername", "ok", "true");
            return chatInfo;
        } catch (CallNotPermittedException e) {
            throw new DomainException(SERVICE_UNAVAILABLE,
                    "Telegram API circuit breaker open for @"
                            + username);
        }
    }

    @Override
    @NonNull
    public ChatMemberInfo getChatMember(long channelId,
            long userId) {
        acquireOrThrow(channelId);

        try {
            var response = sender.execute(
                    new GetChatMember(channelId, userId));
            checkResponse(response, channelId);
            metrics.incrementCounter(CHANNEL_API_CALL,
                    "method", "getChatMember", "ok", "true");
            return converters.toChatMemberInfo(response.chatMember());
        } catch (CallNotPermittedException e) {
            throw new DomainException(SERVICE_UNAVAILABLE,
                    "Telegram API circuit breaker open for channel "
                            + channelId);
        }
    }

    @Override
    @NonNull
    public List<ChatMemberInfo> getChatAdministrators(
            long channelId) {
        var cached = cache.getAdministrators(channelId);
        if (cached.isPresent()) {
            metrics.incrementCounter(CHANNEL_CACHE_HIT,
                    "method", "getChatAdministrators");
            return cached.get();
        }
        metrics.incrementCounter(CHANNEL_CACHE_MISS,
                "method", "getChatAdministrators");

        acquireOrThrow(channelId);

        try {
            var response = sender.execute(
                    new GetChatAdministrators(channelId));
            checkResponse(response, channelId);
            var admins = response.administrators().stream()
                    .map(converters::toChatMemberInfo)
                    .toList();
            cache.putAdministrators(channelId, admins);
            metrics.incrementCounter(CHANNEL_API_CALL,
                    "method", "getChatAdministrators", "ok", "true");
            return admins;
        } catch (CallNotPermittedException e) {
            return fallbackAdministrators(channelId);
        }
    }

    @Override
    public int getChatMemberCount(long channelId) {
        acquireOrThrow(channelId);

        try {
            var response = sender.execute(
                    new GetChatMemberCount(channelId));
            checkResponse(response, channelId);
            metrics.incrementCounter(CHANNEL_API_CALL,
                    "method", "getChatMemberCount", "ok", "true");
            return response.count();
        } catch (CallNotPermittedException e) {
            throw new DomainException(SERVICE_UNAVAILABLE,
                    "Telegram API circuit breaker open for channel "
                            + channelId);
        }
    }

    private void acquireOrThrow(long channelId) {
        if (!rateLimiter.acquire(channelId)) {
            throw new DomainException(
                    RATE_LIMIT_EXCEEDED,
                    "Channel API rate limit exceeded for "
                            + "channel " + channelId);
        }
    }

    private void checkResponse(BaseResponse response,
            long channelId) {
        if (response.isOk()) {
            return;
        }
        metrics.incrementCounter(CHANNEL_API_CALL,
                "method", "unknown", "ok", "false");
        mapTelegramError(response.errorCode(),
                response.description(), channelId);
    }

    private static void mapTelegramError(int errorCode,
            String description, long channelId) {
        if (errorCode == HttpStatus.BAD_REQUEST.value()) {
            throw new DomainException(CHANNEL_NOT_FOUND,
                    "Channel not found: " + channelId);
        }
        if (errorCode == HttpStatus.FORBIDDEN.value()) {
            throw new DomainException(CHANNEL_BOT_NOT_MEMBER,
                    "Bot is not a member of channel " + channelId);
        }
        throw new DomainException(SERVICE_UNAVAILABLE,
                "Telegram API error " + errorCode
                        + " for channel " + channelId
                        + ": " + description);
    }

    private ChatInfo fallbackChatInfo(long channelId) {
        return cache.getChatInfo(channelId)
                .map(stale -> {
                    log.warn("Returning stale cache for channel={}",
                            channelId);
                    metrics.incrementCounter(CHANNEL_CACHE_STALE,
                            "method", "getChat");
                    return stale;
                })
                .orElseThrow(() -> new DomainException(
                        SERVICE_UNAVAILABLE,
                        "Telegram API unavailable and no cached data "
                                + "for channel " + channelId));
    }

    private List<ChatMemberInfo> fallbackAdministrators(
            long channelId) {
        return cache.getAdministrators(channelId)
                .map(stale -> {
                    log.warn("Returning stale admins cache "
                            + "for channel={}", channelId);
                    metrics.incrementCounter(CHANNEL_CACHE_STALE,
                            "method", "getChatAdministrators");
                    return stale;
                })
                .orElseThrow(() -> new DomainException(
                        SERVICE_UNAVAILABLE,
                        "Telegram API unavailable and no cached admins "
                                + "for channel " + channelId));
    }
}

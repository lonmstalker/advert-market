package com.advertmarket.communication.channel.internal;

import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.shared.json.JsonException;
import com.advertmarket.shared.json.JsonFacade;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed implementation of {@link ChannelCachePort}.
 *
 * <p>Uses fail-open strategy: Redis errors are logged and treated
 * as cache misses to avoid cascading failures.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChannelCache implements ChannelCachePort {

    private static final String CHAT_KEY_INFIX = "chat:";
    private static final String ADMINS_KEY_INFIX = "admins:";

    private final StringRedisTemplate redis;
    private final JsonFacade jsonFacade;
    private final ChannelCacheProperties properties;

    @Override
    @NonNull
    public Optional<ChatInfo> getChatInfo(long channelId) {
        try {
            String json = redis.opsForValue()
                    .get(chatKey(channelId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(
                    jsonFacade.fromJson(json, ChatInfo.class));
        } catch (DataAccessException | JsonException e) {
            log.warn("Error reading chat info for channel={}",
                    channelId, e);
            return Optional.empty();
        }
    }

    @Override
    public void putChatInfo(long channelId,
            @NonNull ChatInfo chatInfo) {
        try {
            redis.opsForValue().set(
                    chatKey(channelId),
                    jsonFacade.toJson(chatInfo),
                    properties.chatInfoTtl());
        } catch (DataAccessException e) {
            log.warn("Redis error caching chat info for channel={}",
                    channelId, e);
        }
    }

    @Override
    @NonNull
    public Optional<List<ChatMemberInfo>> getAdministrators(
            long channelId) {
        try {
            String json = redis.opsForValue()
                    .get(adminsKey(channelId));
            if (json == null) {
                return Optional.empty();
            }
            var entry = jsonFacade.fromJson(json,
                    AdminsCacheEntry.class);
            return Optional.of(entry.admins());
        } catch (DataAccessException | JsonException e) {
            log.warn("Error reading admins for channel={}",
                    channelId, e);
            return Optional.empty();
        }
    }

    @Override
    public void putAdministrators(long channelId,
            @NonNull List<ChatMemberInfo> admins) {
        try {
            redis.opsForValue().set(
                    adminsKey(channelId),
                    jsonFacade.toJson(new AdminsCacheEntry(admins)),
                    properties.adminsTtl());
        } catch (DataAccessException e) {
            log.warn("Redis error caching admins for channel={}",
                    channelId, e);
        }
    }

    @Override
    public void evict(long channelId) {
        try {
            redis.delete(List.of(
                    chatKey(channelId),
                    adminsKey(channelId)));
        } catch (DataAccessException e) {
            log.warn("Redis error evicting cache for channel={}",
                    channelId, e);
        }
    }

    private String chatKey(long channelId) {
        return properties.keyPrefix()
                + CHAT_KEY_INFIX + channelId;
    }

    private String adminsKey(long channelId) {
        return properties.keyPrefix()
                + ADMINS_KEY_INFIX + channelId;
    }

    /**
     * Wrapper record to avoid JavaType complexity when
     * deserializing {@code List<ChatMemberInfo>}.
     */
    record AdminsCacheEntry(List<ChatMemberInfo> admins) {
    }
}

package com.advertmarket.communication.channel.internal;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed rate limiter for Telegram Channel API calls.
 *
 * <p>Enforces at most 1 request per second per channel using
 * {@code SET NX PX 1000}. Uses fail-open strategy: Redis errors
 * allow the request to proceed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChannelRateLimiter implements ChannelRateLimiterPort {

    private static final String KEY_PREFIX = "tg:chan:rl:";
    private static final Duration WINDOW = Duration.ofSeconds(1);

    private final StringRedisTemplate redis;

    @Override
    public boolean acquire(long channelId) {
        try {
            Boolean set = redis.opsForValue()
                    .setIfAbsent(KEY_PREFIX + channelId, "1", WINDOW);
            return Boolean.TRUE.equals(set);
        } catch (DataAccessException e) {
            log.warn("Redis error in channel rate limiter "
                    + "for channel={}, allowing request", channelId, e);
            return true;
        }
    }
}

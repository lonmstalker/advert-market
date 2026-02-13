package com.advertmarket.integration.support;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Shared Redis support for integration tests.
 *
 * <p>Uses {@link SharedContainers#REDIS} singleton. Provides a shared
 * {@link LettuceConnectionFactory} and convenience methods.
 */
public final class RedisSupport {

    private static volatile LettuceConnectionFactory sharedFactory;

    private RedisSupport() {
    }

    /**
     * Returns a singleton LettuceConnectionFactory. Thread-safe.
     */
    public static LettuceConnectionFactory connectionFactory() {
        if (sharedFactory == null) {
            synchronized (RedisSupport.class) {
                if (sharedFactory == null) {
                    var factory = new LettuceConnectionFactory(
                            SharedContainers.redisHost(),
                            SharedContainers.redisPort());
                    factory.afterPropertiesSet();
                    sharedFactory = factory;
                }
            }
        }
        return sharedFactory;
    }

    /**
     * Creates a new StringRedisTemplate using the shared connection factory.
     */
    public static StringRedisTemplate redisTemplate() {
        return new StringRedisTemplate(connectionFactory());
    }

    /**
     * Flushes all data from Redis.
     */
    public static void flushAll() {
        connectionFactory().getConnection()
                .serverCommands().flushAll();
    }
}

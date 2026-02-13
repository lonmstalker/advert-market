package com.advertmarket.integration.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.identity.adapter.RedisTokenBlacklist;
import com.advertmarket.integration.support.RedisSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Integration test for RedisTokenBlacklist with real Redis.
 */
@DisplayName("RedisTokenBlacklist — Redis integration")
class RedisTokenBlacklistIntegrationTest {

    private RedisTokenBlacklist blacklist;
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate = RedisSupport.redisTemplate();
        blacklist = new RedisTokenBlacklist(redisTemplate);
    }

    @Test
    @DisplayName("Should detect blacklisted token")
    void shouldDetectBlacklistedToken() {
        blacklist.blacklist("jti-abc", 60);

        assertThat(blacklist.isBlacklisted("jti-abc")).isTrue();
    }

    @Test
    @DisplayName("Should not report non-blacklisted token")
    void shouldNotReportNonBlacklisted() {
        assertThat(blacklist.isBlacklisted("jti-unknown")).isFalse();
    }

    @Test
    @DisplayName("Should store key with TTL in Redis")
    void shouldStoreWithTtl() {
        blacklist.blacklist("jti-ttl", 120);

        String value = redisTemplate.opsForValue()
                .get("jwt:blacklist:jti-ttl");
        assertThat(value).isEqualTo("1");

        Long ttl = redisTemplate.getExpire("jwt:blacklist:jti-ttl");
        assertThat(ttl).isNotNull().isPositive()
                .isLessThanOrEqualTo(120);
    }

    @Test
    @DisplayName("Should expire blacklisted token after TTL")
    void shouldExpireAfterTtl() throws Exception {
        blacklist.blacklist("jti-expire", 1);

        assertThat(blacklist.isBlacklisted("jti-expire")).isTrue();

        Thread.sleep(1500);

        assertThat(blacklist.isBlacklisted("jti-expire")).isFalse();
    }
}

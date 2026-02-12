package com.advertmarket.communication.channel.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChannelCacheProperties")
class RedisChannelCacheTest {

    @Test
    @DisplayName("Default values are applied correctly")
    void defaultValues() {
        var props = new ChannelCacheProperties(
                Duration.ofMinutes(5), Duration.ofMinutes(15),
                "tg:chan:cache:");

        assertThat(props.chatInfoTtl())
                .isEqualTo(Duration.ofMinutes(5));
        assertThat(props.adminsTtl())
                .isEqualTo(Duration.ofMinutes(15));
        assertThat(props.keyPrefix())
                .isEqualTo("tg:chan:cache:");
    }

    @Test
    @DisplayName("Custom values are accepted")
    void customValues() {
        var props = new ChannelCacheProperties(
                Duration.ofMinutes(10), Duration.ofMinutes(30),
                "custom:");

        assertThat(props.chatInfoTtl())
                .isEqualTo(Duration.ofMinutes(10));
        assertThat(props.adminsTtl())
                .isEqualTo(Duration.ofMinutes(30));
        assertThat(props.keyPrefix()).isEqualTo("custom:");
    }
}

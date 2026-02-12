package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChannelId value object")
class ChannelIdTest {

    @Test
    @DisplayName("Negative value is accepted (Telegram channels)")
    void negativeValue_isAccepted() {
        ChannelId channelId = new ChannelId(-1001234567890L);
        assertThat(channelId.value())
                .isEqualTo(-1001234567890L);
    }

    @Test
    @DisplayName("Positive value is accepted")
    void positiveValue_isAccepted() {
        ChannelId channelId = new ChannelId(12345L);
        assertThat(channelId.value()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("Zero is rejected")
    void zero_isRejected() {
        assertThatThrownBy(() -> new ChannelId(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-zero");
    }

    @Test
    @DisplayName("toString returns string representation")
    void toString_returnsStringValue() {
        assertThat(new ChannelId(-100L).toString())
                .isEqualTo("-100");
    }
}

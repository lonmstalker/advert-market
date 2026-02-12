package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ActorType enum")
class ActorTypeTest {

    @Test
    @DisplayName("Has exactly 5 actor types")
    void hasExpectedCount() {
        assertThat(ActorType.values()).hasSize(5);
    }

    @Test
    @DisplayName("Contains all expected actor types")
    void containsAllExpectedTypes() {
        assertThat(ActorType.valueOf("ADVERTISER"))
                .isNotNull();
        assertThat(ActorType.valueOf("CHANNEL_OWNER"))
                .isNotNull();
        assertThat(ActorType.valueOf("CHANNEL_ADMIN"))
                .isNotNull();
        assertThat(ActorType.valueOf("PLATFORM_OPERATOR"))
                .isNotNull();
        assertThat(ActorType.valueOf("SYSTEM")).isNotNull();
    }
}

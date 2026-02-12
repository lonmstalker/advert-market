package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserId value object")
class UserIdTest {

    @Test
    @DisplayName("Positive value is accepted")
    void positiveValue_isAccepted() {
        UserId userId = new UserId(123456789L);
        assertThat(userId.value()).isEqualTo(123456789L);
    }

    @Test
    @DisplayName("Zero is rejected")
    void zero_isRejected() {
        assertThatThrownBy(() -> new UserId(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("Negative value is rejected")
    void negativeValue_isRejected() {
        assertThatThrownBy(() -> new UserId(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toString returns string representation")
    void toString_returnsStringValue() {
        assertThat(new UserId(42).toString()).isEqualTo("42");
    }
}

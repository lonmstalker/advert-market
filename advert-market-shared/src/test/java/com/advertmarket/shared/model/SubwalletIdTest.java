package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubwalletId — TON subwallet identifier")
class SubwalletIdTest {

    @Test
    @DisplayName("Stores non-negative value")
    void createsWithValue() {
        var id = new SubwalletId(12345L);
        assertThat(id.value()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("Allows zero value")
    void allowsZero() {
        var id = new SubwalletId(0L);
        assertThat(id.value()).isZero();
    }

    @Test
    @DisplayName("Rejects negative value")
    void rejectsNegative() {
        assertThatThrownBy(() -> new SubwalletId(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Equality based on value")
    void equality() {
        assertThat(new SubwalletId(42L))
                .isEqualTo(new SubwalletId(42L));
    }
}

package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TonAddress — TON wallet address value object")
class TonAddressTest {

    @Test
    @DisplayName("Stores user-friendly address")
    void createsWithUserFriendlyAddress() {
        var addr = new TonAddress("UQBvW8Z5huBkMJYd...");
        assertThat(addr.value()).startsWith("UQ");
    }

    @Test
    @DisplayName("Stores raw address")
    void createsWithRawAddress() {
        var addr = new TonAddress("0:abc123");
        assertThat(addr.value()).isEqualTo("0:abc123");
    }

    @Test
    @DisplayName("Rejects null value")
    void rejectsNull() {
        assertThatThrownBy(() -> new TonAddress(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Rejects blank value")
    void rejectsBlank() {
        assertThatThrownBy(() -> new TonAddress(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Equality based on value")
    void equality() {
        assertThat(new TonAddress("EQabc"))
                .isEqualTo(new TonAddress("EQabc"));
    }
}

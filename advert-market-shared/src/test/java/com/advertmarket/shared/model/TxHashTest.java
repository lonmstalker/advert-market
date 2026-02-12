package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TxHash — TON transaction hash value object")
class TxHashTest {

    @Test
    @DisplayName("Stores and returns the hash value")
    void createsWithValue() {
        var hash = new TxHash("abc123def456");
        assertThat(hash.value()).isEqualTo("abc123def456");
    }

    @Test
    @DisplayName("Rejects null value")
    void rejectsNull() {
        assertThatThrownBy(() -> new TxHash(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Rejects blank value")
    void rejectsBlank() {
        assertThatThrownBy(() -> new TxHash("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toString returns the hash value")
    void toStringReturnsValue() {
        assertThat(new TxHash("abc").toString()).isEqualTo("abc");
    }

    @Test
    @DisplayName("Equality based on value")
    void equality() {
        assertThat(new TxHash("abc")).isEqualTo(new TxHash("abc"));
        assertThat(new TxHash("abc")).isNotEqualTo(new TxHash("def"));
    }
}

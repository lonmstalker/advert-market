package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TxHash — TON transaction hash value object")
class TxHashTest {

    private static final String HEX_HASH =
            "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"
            + "e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";
    private static final String BASE64_HASH =
            "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=";

    @Test
    @DisplayName("Stores and returns hex hash value")
    void createsWithHexValue() {
        var hash = new TxHash(HEX_HASH);
        assertThat(hash.value()).isEqualTo(HEX_HASH);
    }

    @Test
    @DisplayName("Stores and returns base64 hash value")
    void createsWithBase64Value() {
        var hash = new TxHash(BASE64_HASH);
        assertThat(hash.value()).isEqualTo(BASE64_HASH);
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
    @DisplayName("Rejects invalid format")
    void rejectsInvalidFormat() {
        assertThatThrownBy(() -> new TxHash("abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toString returns the hash value")
    void toStringReturnsValue() {
        assertThat(new TxHash(HEX_HASH).toString())
                .isEqualTo(HEX_HASH);
    }

    @Test
    @DisplayName("Equality based on value")
    void equality() {
        assertThat(new TxHash(HEX_HASH))
                .isEqualTo(new TxHash(HEX_HASH));
        assertThat(new TxHash(HEX_HASH))
                .isNotEqualTo(new TxHash(BASE64_HASH));
    }
}
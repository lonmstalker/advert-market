package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TonAddress — TON wallet address value object")
class TonAddressTest {

    private static final String USER_FRIENDLY =
            "UQBvW8Z5huBkMJYdnJxrERhVfeLsvKVbcjOx0Z3KPnEr0dSx";
    private static final String RAW =
            "0:6f5bc6798ae06430961d9c9c6b111855"
            + "7de2ecbca55b7233b1d19dca3e712bd1";

    @Test
    @DisplayName("Stores user-friendly address")
    void createsWithUserFriendlyAddress() {
        var addr = new TonAddress(USER_FRIENDLY);
        assertThat(addr.value()).startsWith("UQ");
    }

    @Test
    @DisplayName("Stores raw address")
    void createsWithRawAddress() {
        var addr = new TonAddress(RAW);
        assertThat(addr.value()).isEqualTo(RAW);
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
    @DisplayName("Rejects invalid format")
    void rejectsInvalidFormat() {
        assertThatThrownBy(() -> new TonAddress("not-an-address"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Equality based on value")
    void equality() {
        assertThat(new TonAddress(USER_FRIENDLY))
                .isEqualTo(new TonAddress(USER_FRIENDLY));
    }
}
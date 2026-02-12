package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("AccountType enum")
class AccountTypeTest {

    @Test
    @DisplayName("Has exactly 10 account types")
    void hasExpectedCount() {
        assertThat(AccountType.values()).hasSize(10);
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class,
            names = {"PLATFORM_TREASURY", "EXTERNAL_TON",
                    "NETWORK_FEES", "DUST_WRITEOFF"})
    @DisplayName("Singleton account types")
    void singletons(AccountType type) {
        assertThat(type.isSingleton()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class,
            names = {"ESCROW", "OWNER_PENDING", "COMMISSION",
                    "OVERPAYMENT", "PARTIAL_DEPOSIT",
                    "LATE_DEPOSIT"})
    @DisplayName("Non-singleton account types")
    void nonSingletons(AccountType type) {
        assertThat(type.isSingleton()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class,
            names = {"EXTERNAL_TON", "NETWORK_FEES",
                    "DUST_WRITEOFF"})
    @DisplayName("Contra/expense accounts allow negative balance")
    void allowNegativeBalance(AccountType type) {
        assertThat(type.requiresNonNegativeBalance()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class,
            names = {"PLATFORM_TREASURY", "ESCROW",
                    "OWNER_PENDING", "COMMISSION",
                    "OVERPAYMENT", "PARTIAL_DEPOSIT",
                    "LATE_DEPOSIT"})
    @DisplayName("Regular accounts require non-negative balance")
    void requireNonNegativeBalance(AccountType type) {
        assertThat(type.requiresNonNegativeBalance()).isTrue();
    }
}

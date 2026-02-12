package com.advertmarket.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InsufficientBalanceException")
class InsufficientBalanceExceptionTest {

    @Test
    @DisplayName("Contains account and balance details")
    void containsBalanceDetails() {
        var accountId = AccountId.platformTreasury();
        var requested = Money.ofTon(10);
        var available = Money.ofTon(5);

        var ex = new InsufficientBalanceException(
                accountId, requested, available);

        assertThat(ex.getErrorCode())
                .isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(ex.getMessage())
                .contains("PLATFORM_TREASURY");
        assertThat(ex.getAccountId()).isEqualTo(accountId);
        assertThat(ex.getRequested()).isEqualTo(requested);
        assertThat(ex.getAvailable()).isEqualTo(available);
    }

    @Test
    @DisplayName("Context contains structured data")
    void contextContainsStructuredData() {
        var ex = new InsufficientBalanceException(
                AccountId.externalTon(),
                Money.ofNano(100),
                Money.ofNano(50));

        assertThat(ex.getContext())
                .containsEntry("accountId", "EXTERNAL_TON")
                .containsEntry("requested", 100L)
                .containsEntry("available", 50L);
    }

    @Test
    @DisplayName("Is a DomainException")
    void isDomainException() {
        var ex = new InsufficientBalanceException(
                AccountId.externalTon(),
                Money.ofNano(1),
                Money.zero());
        assertThat(ex).isInstanceOf(DomainException.class);
    }
}

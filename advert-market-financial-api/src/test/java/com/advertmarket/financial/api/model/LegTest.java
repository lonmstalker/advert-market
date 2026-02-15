package com.advertmarket.financial.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Leg")
class LegTest {

    private static final AccountId ESCROW =
            AccountId.escrow(DealId.generate());

    @Test
    @DisplayName("Should create a valid debit leg")
    void validDebitLeg() {
        var leg = new Leg(ESCROW, EntryType.ESCROW_DEPOSIT,
                Money.ofNano(1000), Leg.Side.DEBIT);

        assertThat(leg.debitNano()).isEqualTo(1000);
        assertThat(leg.creditNano()).isZero();
        assertThat(leg.isDebit()).isTrue();
        assertThat(leg.isCredit()).isFalse();
    }

    @Test
    @DisplayName("Should create a valid credit leg")
    void validCreditLeg() {
        var leg = new Leg(ESCROW, EntryType.ESCROW_DEPOSIT,
                Money.ofNano(2000), Leg.Side.CREDIT);

        assertThat(leg.debitNano()).isZero();
        assertThat(leg.creditNano()).isEqualTo(2000);
        assertThat(leg.isCredit()).isTrue();
        assertThat(leg.isDebit()).isFalse();
    }

    @Test
    @DisplayName("Should reject zero amount")
    void rejectZeroAmount() {
        assertThatThrownBy(() -> new Leg(ESCROW,
                EntryType.ESCROW_DEPOSIT, Money.zero(), Leg.Side.DEBIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("Should reject null accountId")
    void rejectNullAccountId() {
        assertThatThrownBy(() -> new Leg(null,
                EntryType.ESCROW_DEPOSIT, Money.ofNano(100),
                Leg.Side.DEBIT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should reject null entryType")
    void rejectNullEntryType() {
        assertThatThrownBy(() -> new Leg(ESCROW, null,
                Money.ofNano(100), Leg.Side.DEBIT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should reject null amount")
    void rejectNullAmount() {
        assertThatThrownBy(() -> new Leg(ESCROW,
                EntryType.ESCROW_DEPOSIT, null, Leg.Side.DEBIT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should reject null side")
    void rejectNullSide() {
        assertThatThrownBy(() -> new Leg(ESCROW,
                EntryType.ESCROW_DEPOSIT, Money.ofNano(100), null))
                .isInstanceOf(NullPointerException.class);
    }
}

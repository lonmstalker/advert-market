package com.advertmarket.financial.api.model;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LedgerEntry")
class LedgerEntryTest {

    private static final AccountId ACCOUNT =
            AccountId.escrow(DealId.generate());
    private static final UUID TX_REF = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Test
    @DisplayName("Should create valid debit entry")
    void validDebit() {
        assertThatNoException().isThrownBy(() ->
                new LedgerEntry(1L, DealId.generate(), ACCOUNT,
                        EntryType.ESCROW_DEPOSIT, 1000, 0,
                        "key", TX_REF, null, NOW));
    }

    @Test
    @DisplayName("Should create valid credit entry")
    void validCredit() {
        assertThatNoException().isThrownBy(() ->
                new LedgerEntry(1L, null, ACCOUNT,
                        EntryType.ESCROW_DEPOSIT, 0, 1000,
                        "key", TX_REF, "desc", NOW));
    }

    @Test
    @DisplayName("Should reject both debit and credit being zero")
    void rejectBothZero() {
        assertThatThrownBy(() ->
                new LedgerEntry(1L, null, ACCOUNT,
                        EntryType.ESCROW_DEPOSIT, 0, 0,
                        "key", TX_REF, null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one");
    }

    @Test
    @DisplayName("Should reject both debit and credit being positive")
    void rejectBothPositive() {
        assertThatThrownBy(() ->
                new LedgerEntry(1L, null, ACCOUNT,
                        EntryType.ESCROW_DEPOSIT, 100, 200,
                        "key", TX_REF, null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one");
    }

    @Test
    @DisplayName("Should reject negative debitNano")
    void rejectNegativeDebit() {
        assertThatThrownBy(() ->
                new LedgerEntry(1L, null, ACCOUNT,
                        EntryType.ESCROW_DEPOSIT, -1, 0,
                        "key", TX_REF, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject null accountId")
    void rejectNullAccountId() {
        assertThatThrownBy(() ->
                new LedgerEntry(1L, null, null,
                        EntryType.ESCROW_DEPOSIT, 100, 0,
                        "key", TX_REF, null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should reject null txRef")
    void rejectNullTxRef() {
        assertThatThrownBy(() ->
                new LedgerEntry(1L, null, ACCOUNT,
                        EntryType.ESCROW_DEPOSIT, 100, 0,
                        "key", null, null, NOW))
                .isInstanceOf(NullPointerException.class);
    }
}

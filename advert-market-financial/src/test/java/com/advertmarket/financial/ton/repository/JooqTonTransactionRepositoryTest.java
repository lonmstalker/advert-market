package com.advertmarket.financial.ton.repository;

import static com.advertmarket.db.generated.tables.TonTransactions.TON_TRANSACTIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JooqTonTransactionRepository — structural tests")
class JooqTonTransactionRepositoryTest {

    private JooqTonTransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JooqTonTransactionRepository(mock(DSLContext.class));
    }

    @Test
    @DisplayName("Should be instantiable with DSLContext")
    void instantiable() {
        assertThat(repository).isNotNull();
    }

    @Test
    @DisplayName("TON_TRANSACTIONS table has required columns")
    void tableHasRequiredColumns() {
        assertThat(TON_TRANSACTIONS.ID).isNotNull();
        assertThat(TON_TRANSACTIONS.DEAL_ID).isNotNull();
        assertThat(TON_TRANSACTIONS.TX_HASH).isNotNull();
        assertThat(TON_TRANSACTIONS.DIRECTION).isNotNull();
        assertThat(TON_TRANSACTIONS.AMOUNT_NANO).isNotNull();
        assertThat(TON_TRANSACTIONS.FROM_ADDRESS).isNotNull();
        assertThat(TON_TRANSACTIONS.TO_ADDRESS).isNotNull();
        assertThat(TON_TRANSACTIONS.STATUS).isNotNull();
        assertThat(TON_TRANSACTIONS.CONFIRMATIONS).isNotNull();
        assertThat(TON_TRANSACTIONS.SUBWALLET_ID).isNotNull();
        assertThat(TON_TRANSACTIONS.VERSION).isNotNull();
        assertThat(TON_TRANSACTIONS.SEQNO).isNotNull();
        assertThat(TON_TRANSACTIONS.TX_TYPE).isNotNull();
        assertThat(TON_TRANSACTIONS.FEE_NANO).isNotNull();
        assertThat(TON_TRANSACTIONS.CREATED_AT).isNotNull();
        assertThat(TON_TRANSACTIONS.CONFIRMED_AT).isNotNull();
    }
}
package com.advertmarket.integration.financial;

import static com.advertmarket.db.generated.tables.LedgerEntries.LEDGER_ENTRIES;
import static com.advertmarket.db.generated.tables.LedgerIdempotencyKeys.LEDGER_IDEMPOTENCY_KEYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.integration.support.DatabaseSupport;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests verifying DDL-level constraints on financial tables.
 * PG-only — no Spring context required.
 */
@DisplayName("Ledger DB constraints — PostgreSQL integration")
class LedgerDbConstraintsIntegrationTest {

    private static DSLContext dsl;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
        dsl = DatabaseSupport.dsl();
    }

    @BeforeEach
    void cleanUp() {
        DatabaseSupport.cleanFinancialTables(dsl);
    }

    private UUID insertTestEntry(long debitNano, long creditNano) {
        String key = "test:" + UUID.randomUUID();
        dsl.insertInto(LEDGER_IDEMPOTENCY_KEYS)
                .set(LEDGER_IDEMPOTENCY_KEYS.IDEMPOTENCY_KEY, key)
                .execute();

        UUID txRef = UUID.randomUUID();
        dsl.insertInto(LEDGER_ENTRIES)
                .set(LEDGER_ENTRIES.TX_REF, txRef)
                .set(LEDGER_ENTRIES.IDEMPOTENCY_KEY, key)
                .set(LEDGER_ENTRIES.ACCOUNT_ID, "TEST_ACCOUNT")
                .set(LEDGER_ENTRIES.ENTRY_TYPE, "ESCROW_DEPOSIT")
                .set(LEDGER_ENTRIES.DEBIT_NANO, debitNano)
                .set(LEDGER_ENTRIES.CREDIT_NANO, creditNano)
                .execute();
        return txRef;
    }

    @Nested
    @DisplayName("Immutability trigger")
    class ImmutabilityTrigger {

        @Test
        @DisplayName("Should reject UPDATE on ledger_entries")
        void rejectUpdate() {
            UUID txRef = insertTestEntry(ONE_TON, 0L);

            assertThatThrownBy(() ->
                    dsl.update(LEDGER_ENTRIES)
                            .set(LEDGER_ENTRIES.DEBIT_NANO, 999L)
                            .where(LEDGER_ENTRIES.TX_REF.eq(txRef))
                            .execute())
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        @DisplayName("Should reject DELETE on ledger_entries")
        void rejectDelete() {
            insertTestEntry(ONE_TON, 0L);

            assertThatThrownBy(() ->
                    dsl.deleteFrom(LEDGER_ENTRIES).execute())
                    .isInstanceOf(DataAccessException.class);
        }
    }

    @Nested
    @DisplayName("Check constraints")
    class CheckConstraints {

        @Test
        @DisplayName("Should reject negative debit_nano")
        void negativeDebit() {
            assertThatThrownBy(() -> insertTestEntry(-1L, 0L))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        @DisplayName("Should reject negative credit_nano")
        void negativeCredit() {
            assertThatThrownBy(() -> insertTestEntry(0L, -1L))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        @DisplayName("Should reject both debit and credit positive")
        void bothPositive() {
            assertThatThrownBy(() -> insertTestEntry(100L, 100L))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        @DisplayName("Should reject both debit and credit zero")
        void bothZero() {
            assertThatThrownBy(() -> insertTestEntry(0L, 0L))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        @DisplayName("Should accept valid debit entry (debit > 0, credit = 0)")
        void validDebit() {
            UUID txRef = insertTestEntry(ONE_TON, 0L);

            int count = dsl.selectCount()
                    .from(LEDGER_ENTRIES)
                    .where(LEDGER_ENTRIES.TX_REF.eq(txRef))
                    .fetchOne(0, int.class);
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should accept valid credit entry (debit = 0, credit > 0)")
        void validCredit() {
            UUID txRef = insertTestEntry(0L, ONE_TON);

            int count = dsl.selectCount()
                    .from(LEDGER_ENTRIES)
                    .where(LEDGER_ENTRIES.TX_REF.eq(txRef))
                    .fetchOne(0, int.class);
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Idempotency key constraint")
    class IdempotencyKeyConstraint {

        @Test
        @DisplayName("Should reject duplicate PK in ledger_idempotency_keys")
        void duplicatePk() {
            String key = "deposit:pk-dup";
            dsl.insertInto(LEDGER_IDEMPOTENCY_KEYS)
                    .set(LEDGER_IDEMPOTENCY_KEYS.IDEMPOTENCY_KEY, key)
                    .execute();

            assertThatThrownBy(() ->
                    dsl.insertInto(LEDGER_IDEMPOTENCY_KEYS)
                            .set(LEDGER_IDEMPOTENCY_KEYS.IDEMPOTENCY_KEY,
                                    key)
                            .execute())
                    .isInstanceOf(DataAccessException.class);
        }
    }

    private static final long ONE_TON = 1_000_000_000L;
}

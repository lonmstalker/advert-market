package com.advertmarket.integration.financial;

import static com.advertmarket.db.generated.tables.AccountBalances.ACCOUNT_BALANCES;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.financial.ledger.repository.JooqAccountBalanceRepository;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.UserId;
import java.util.OptionalLong;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link JooqAccountBalanceRepository}.
 * PG-only — no Spring context required.
 */
@DisplayName("JooqAccountBalanceRepository — PostgreSQL integration")
class JooqAccountBalanceRepositoryIntegrationTest {

    private static DSLContext dsl;
    private static JooqAccountBalanceRepository repository;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
        dsl = DatabaseSupport.dsl();
        repository = new JooqAccountBalanceRepository(dsl);
    }

    @BeforeEach
    void cleanUp() {
        DatabaseSupport.cleanFinancialTables(dsl);
    }

    @Nested
    @DisplayName("upsertBalanceUnchecked")
    class UpsertBalanceUnchecked {

        @Test
        @DisplayName("Should create row with delta as initial balance on first call")
        void firstCallCreatesRow() {
            AccountId account = AccountId.escrow(DealId.generate());

            long result = repository.upsertBalanceUnchecked(
                    account, 1_000_000_000L);

            assertThat(result).isEqualTo(1_000_000_000L);
            assertThat(repository.getBalance(account))
                    .isEqualTo(1_000_000_000L);
        }

        @Test
        @DisplayName("Should add delta to existing balance on subsequent call")
        void secondCallAddsDelta() {
            AccountId account = AccountId.escrow(DealId.generate());

            repository.upsertBalanceUnchecked(account, 1_000_000_000L);
            long result = repository.upsertBalanceUnchecked(
                    account, 500_000_000L);

            assertThat(result).isEqualTo(1_500_000_000L);
        }

        @Test
        @DisplayName("Should allow negative delta for contra accounts")
        void negativeDelta() {
            AccountId externalTon = AccountId.externalTon();

            long result = repository.upsertBalanceUnchecked(
                    externalTon, -3_000_000_000L);

            assertThat(result).isEqualTo(-3_000_000_000L);
        }

        @Test
        @DisplayName("Should increment version on each update")
        void versionIncrement() {
            AccountId account = AccountId.escrow(DealId.generate());

            repository.upsertBalanceUnchecked(account, 100L);
            Integer v1 = dsl.select(ACCOUNT_BALANCES.VERSION)
                    .from(ACCOUNT_BALANCES)
                    .where(ACCOUNT_BALANCES.ACCOUNT_ID.eq(account.value()))
                    .fetchOne(ACCOUNT_BALANCES.VERSION);
            assertThat(v1).isEqualTo(1);

            repository.upsertBalanceUnchecked(account, 200L);
            Integer v2 = dsl.select(ACCOUNT_BALANCES.VERSION)
                    .from(ACCOUNT_BALANCES)
                    .where(ACCOUNT_BALANCES.ACCOUNT_ID.eq(account.value()))
                    .fetchOne(ACCOUNT_BALANCES.VERSION);
            assertThat(v2).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle zero delta without error")
        void zeroDelta() {
            AccountId account = AccountId.escrow(DealId.generate());

            long result = repository.upsertBalanceUnchecked(account, 0L);

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Should handle values near Long.MAX_VALUE")
        void nearMaxValue() {
            AccountId account = AccountId.externalTon();

            long result = repository.upsertBalanceUnchecked(
                    account, Long.MAX_VALUE);

            assertThat(result).isEqualTo(Long.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("upsertBalanceNonNegative")
    class UpsertBalanceNonNegative {

        @Test
        @DisplayName("Should return empty for non-existent account")
        void nonExistentAccount() {
            AccountId account = AccountId.escrow(DealId.generate());

            OptionalLong result = repository.upsertBalanceNonNegative(
                    account, 100L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return remaining balance when sufficient funds")
        void sufficientFunds() {
            AccountId account = AccountId.escrow(DealId.generate());
            repository.upsertBalanceUnchecked(account, 1_000_000_000L);

            OptionalLong result = repository.upsertBalanceNonNegative(
                    account, 400_000_000L);

            assertThat(result).hasValue(600_000_000L);
        }

        @Test
        @DisplayName("Should return empty when insufficient funds")
        void insufficientFunds() {
            AccountId account = AccountId.escrow(DealId.generate());
            repository.upsertBalanceUnchecked(account, 100L);

            OptionalLong result = repository.upsertBalanceNonNegative(
                    account, 200L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return zero on exact debit")
        void exactDebit() {
            AccountId account = AccountId.escrow(DealId.generate());
            repository.upsertBalanceUnchecked(account, 500L);

            OptionalLong result = repository.upsertBalanceNonNegative(
                    account, 500L);

            assertThat(result).hasValue(0L);
        }

        @Test
        @DisplayName("Should return empty for zero debit on non-existent account")
        void zeroDebitNonExistent() {
            AccountId account = AccountId.escrow(DealId.generate());

            OptionalLong result = repository.upsertBalanceNonNegative(
                    account, 0L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should increment version on successful debit")
        void versionIncrementOnDebit() {
            AccountId account = AccountId.escrow(DealId.generate());
            repository.upsertBalanceUnchecked(account, 1000L);

            Integer before = dsl.select(ACCOUNT_BALANCES.VERSION)
                    .from(ACCOUNT_BALANCES)
                    .where(ACCOUNT_BALANCES.ACCOUNT_ID.eq(account.value()))
                    .fetchOne(ACCOUNT_BALANCES.VERSION);

            repository.upsertBalanceNonNegative(account, 500L);

            Integer after = dsl.select(ACCOUNT_BALANCES.VERSION)
                    .from(ACCOUNT_BALANCES)
                    .where(ACCOUNT_BALANCES.ACCOUNT_ID.eq(account.value()))
                    .fetchOne(ACCOUNT_BALANCES.VERSION);

            assertThat(after).isEqualTo(before + 1);
        }
    }

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("Should return 0 for non-existent account")
        void nonExistentReturnsZero() {
            AccountId account = AccountId.escrow(DealId.generate());

            long balance = repository.getBalance(account);

            assertThat(balance).isZero();
        }

        @Test
        @DisplayName("Should return current balance for existing account")
        void existingAccountBalance() {
            AccountId account = AccountId.ownerPending(new UserId(42L));
            repository.upsertBalanceUnchecked(account, 750_000_000L);

            long balance = repository.getBalance(account);

            assertThat(balance).isEqualTo(750_000_000L);
        }
    }
}

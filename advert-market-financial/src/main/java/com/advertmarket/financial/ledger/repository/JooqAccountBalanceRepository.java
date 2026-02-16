package com.advertmarket.financial.ledger.repository;

import static com.advertmarket.db.generated.tables.AccountBalances.ACCOUNT_BALANCES;
import static org.jooq.impl.DSL.val;

import com.advertmarket.shared.model.AccountId;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * Repository for account balance projections backed by jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqAccountBalanceRepository {

    private final DSLContext dsl;

    /**
     * UPSERT balance without non-negative check (for contra accounts like EXTERNAL_TON).
     * If the row doesn't exist, inserts with deltaNano as initial balance.
     * If the row exists, adds deltaNano to current balance.
     *
     * @param accountId the account
     * @param deltaNano positive or negative delta
     * @return new balance after update
     */
    public long upsertBalanceUnchecked(
            @NonNull AccountId accountId, long deltaNano) {

        Long balance = dsl.insertInto(ACCOUNT_BALANCES)
                .set(ACCOUNT_BALANCES.ACCOUNT_ID, accountId.value())
                .set(ACCOUNT_BALANCES.BALANCE_NANO, deltaNano)
                .set(ACCOUNT_BALANCES.VERSION, 1)
                .onConflict(ACCOUNT_BALANCES.ACCOUNT_ID)
                .doUpdate()
                .set(ACCOUNT_BALANCES.BALANCE_NANO,
                        ACCOUNT_BALANCES.BALANCE_NANO.plus(val(deltaNano)))
                .set(ACCOUNT_BALANCES.VERSION,
                        ACCOUNT_BALANCES.VERSION.plus(1))
                .returning(ACCOUNT_BALANCES.BALANCE_NANO)
                .fetchSingle(ACCOUNT_BALANCES.BALANCE_NANO);
        return Objects.requireNonNull(balance,
                "balance_nano must not be null after upsert");
    }

    /**
     * Atomic UPSERT with non-negative balance check for debit operations.
     * For new accounts (no row), if debitAmount &gt; 0 the operation fails
     * because initial balance is 0.
     * For existing accounts, atomically checks balance &gt;= debitAmount
     * before subtracting.
     *
     * @param accountId the account
     * @param debitAmount positive amount to subtract
     * @return new balance after debit, or empty if insufficient funds
     */
    public @NonNull OptionalLong upsertBalanceNonNegative(
            @NonNull AccountId accountId, long debitAmount) {

        Long newBalance = dsl.update(ACCOUNT_BALANCES)
                .set(ACCOUNT_BALANCES.BALANCE_NANO,
                        ACCOUNT_BALANCES.BALANCE_NANO.minus(val(debitAmount)))
                .set(ACCOUNT_BALANCES.VERSION,
                        ACCOUNT_BALANCES.VERSION.plus(1))
                .where(ACCOUNT_BALANCES.ACCOUNT_ID.eq(accountId.value()))
                .and(ACCOUNT_BALANCES.BALANCE_NANO.ge(debitAmount))
                .returning(ACCOUNT_BALANCES.BALANCE_NANO)
                .fetchOne(ACCOUNT_BALANCES.BALANCE_NANO);

        return newBalance != null
                ? OptionalLong.of(newBalance)
                : OptionalLong.empty();
    }

    /**
     * Returns current balance for the account, or 0 if no row exists.
     */
    public long getBalance(@NonNull AccountId accountId) {
        Long balance = dsl.select(ACCOUNT_BALANCES.BALANCE_NANO)
                .from(ACCOUNT_BALANCES)
                .where(ACCOUNT_BALANCES.ACCOUNT_ID.eq(accountId.value()))
                .fetchOne(ACCOUNT_BALANCES.BALANCE_NANO);
        return balance != null ? balance : 0L;
    }

    /**
     * Finds commission accounts (prefix "COMMISSION:") with balance above the given threshold.
     *
     * @param thresholdNano minimum balance in nanoTON (exclusive)
     * @param limit max number of accounts to return
     * @return list of account IDs with balance above threshold
     */
    public @NonNull List<AccountId> findCommissionAccountsAboveThreshold(
            long thresholdNano, int limit) {
        return dsl.select(ACCOUNT_BALANCES.ACCOUNT_ID)
                .from(ACCOUNT_BALANCES)
                .where(ACCOUNT_BALANCES.ACCOUNT_ID.startsWith("COMMISSION:"))
                .and(ACCOUNT_BALANCES.BALANCE_NANO.gt(thresholdNano))
                .limit(limit)
                .fetch(r -> new AccountId(r.value1()));
    }
}

package com.advertmarket.financial.ton.repository;

import static com.advertmarket.db.generated.tables.TonTransactions.TON_TRANSACTIONS;

import com.advertmarket.db.generated.tables.records.TonTransactionsRecord;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * Repository for TON blockchain transaction records.
 */
@Repository
@RequiredArgsConstructor
public class JooqTonTransactionRepository {

    private final DSLContext dsl;

    /**
     * Inserts a new transaction record.
     *
     * @param record partially filled record (ID is generated)
     * @return the generated record ID
     */
    public long save(@NonNull TonTransactionsRecord record) {
        return Objects.requireNonNull(
                dsl.insertInto(TON_TRANSACTIONS)
                        .set(record)
                        .returning(TON_TRANSACTIONS.ID)
                        .fetchSingle(TON_TRANSACTIONS.ID));
    }

    /**
     * Finds all transactions for a deal.
     */
    public @NonNull List<TonTransactionsRecord> findByDealId(@NonNull UUID dealId) {
        return dsl.selectFrom(TON_TRANSACTIONS)
                .where(TON_TRANSACTIONS.DEAL_ID.eq(dealId))
                .orderBy(TON_TRANSACTIONS.CREATED_AT.asc())
                .fetchInto(TonTransactionsRecord.class);
    }

    /**
     * Finds pending deposit transactions with FOR UPDATE SKIP LOCKED.
     */
    public @NonNull List<TonTransactionsRecord> findPendingDeposits(int limit) {
        return dsl.selectFrom(TON_TRANSACTIONS)
                .where(TON_TRANSACTIONS.DIRECTION.eq("IN"))
                .and(TON_TRANSACTIONS.STATUS.eq("PENDING"))
                .orderBy(TON_TRANSACTIONS.CREATED_AT.asc())
                .limit(limit)
                .forUpdate()
                .skipLocked()
                .fetchInto(TonTransactionsRecord.class);
    }

    /**
     * CAS update of status with version check.
     *
     * @return true if exactly one row was updated
     */
    public boolean updateStatus(long id,
                                @NonNull String newStatus,
                                int confirmations,
                                int expectedVersion) {
        return dsl.update(TON_TRANSACTIONS)
                .set(TON_TRANSACTIONS.STATUS, newStatus)
                .set(TON_TRANSACTIONS.CONFIRMATIONS, confirmations)
                .set(TON_TRANSACTIONS.VERSION, expectedVersion + 1)
                .where(TON_TRANSACTIONS.ID.eq(id))
                .and(TON_TRANSACTIONS.VERSION.eq(expectedVersion))
                .execute() == 1;
    }

    /**
     * Marks a transaction as confirmed with blockchain details.
     */
    public boolean updateConfirmed(long id,
                                   @NonNull String txHash,
                                   int confirmations,
                                   long feeNano,
                                   @NonNull OffsetDateTime confirmedAt) {
        return dsl.update(TON_TRANSACTIONS)
                .set(TON_TRANSACTIONS.STATUS, "CONFIRMED")
                .set(TON_TRANSACTIONS.TX_HASH, txHash)
                .set(TON_TRANSACTIONS.CONFIRMATIONS, confirmations)
                .set(TON_TRANSACTIONS.FEE_NANO, feeNano)
                .set(TON_TRANSACTIONS.CONFIRMED_AT, confirmedAt)
                .set(TON_TRANSACTIONS.VERSION, TON_TRANSACTIONS.VERSION.plus(1))
                .where(TON_TRANSACTIONS.ID.eq(id))
                .execute() == 1;
    }

    /**
     * Finds a transaction by its blockchain hash (for deduplication).
     */
    public @NonNull Optional<TonTransactionsRecord> findByTxHash(@NonNull String txHash) {
        return dsl.selectFrom(TON_TRANSACTIONS)
                .where(TON_TRANSACTIONS.TX_HASH.eq(txHash))
                .fetchOptionalInto(TonTransactionsRecord.class);
    }

    /**
     * Sets the transaction hash after submission.
     *
     * @return true if exactly one row was updated
     */
    public boolean setTxHash(long id, @NonNull String txHash) {
        return dsl.update(TON_TRANSACTIONS)
                .set(TON_TRANSACTIONS.TX_HASH, txHash)
                .set(TON_TRANSACTIONS.STATUS, "SUBMITTED")
                .set(TON_TRANSACTIONS.VERSION, TON_TRANSACTIONS.VERSION.plus(1))
                .where(TON_TRANSACTIONS.ID.eq(id))
                .execute() == 1;
    }
}
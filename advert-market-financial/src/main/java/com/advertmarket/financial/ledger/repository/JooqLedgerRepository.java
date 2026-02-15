package com.advertmarket.financial.ledger.repository;

import static com.advertmarket.db.generated.tables.LedgerEntries.LEDGER_ENTRIES;
import static com.advertmarket.db.generated.tables.LedgerIdempotencyKeys.LEDGER_IDEMPOTENCY_KEYS;

import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.pagination.CursorPage;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * Repository for ledger entries and idempotency keys backed by jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqLedgerRepository {

    private final DSLContext dsl;

    /**
     * Atomically inserts an idempotency key. Returns {@code true} if this was the first insert
     * (no conflict), {@code false} if the key already exists.
     */
    public boolean tryInsertIdempotencyKey(@NonNull String idempotencyKey) {
        int affected = dsl.insertInto(LEDGER_IDEMPOTENCY_KEYS)
                .set(LEDGER_IDEMPOTENCY_KEYS.IDEMPOTENCY_KEY, idempotencyKey)
                .onConflictDoNothing()
                .execute();
        return affected > 0;
    }

    /**
     * Finds the transaction reference for a previously recorded idempotency key.
     */
    public @NonNull Optional<UUID> findTxRefByIdempotencyKey(
            @NonNull String idempotencyKey) {
        return dsl.select(LEDGER_ENTRIES.TX_REF)
                .from(LEDGER_ENTRIES)
                .where(LEDGER_ENTRIES.IDEMPOTENCY_KEY.eq(idempotencyKey))
                .limit(1)
                .fetchOptional(LEDGER_ENTRIES.TX_REF);
    }

    /**
     * Batch-inserts all legs as ledger entries under a single tx_ref.
     */
    public void insertEntries(
            @NonNull UUID txRef,
            @NonNull String idempotencyKey,
            @Nullable DealId dealId,
            @Nullable String description,
            @NonNull List<Leg> legs) {

        UUID dealUuid = dealId != null ? dealId.value() : null;
        var insert = dsl.insertInto(LEDGER_ENTRIES,
                LEDGER_ENTRIES.TX_REF,
                LEDGER_ENTRIES.IDEMPOTENCY_KEY,
                LEDGER_ENTRIES.DEAL_ID,
                LEDGER_ENTRIES.ACCOUNT_ID,
                LEDGER_ENTRIES.ENTRY_TYPE,
                LEDGER_ENTRIES.DEBIT_NANO,
                LEDGER_ENTRIES.CREDIT_NANO,
                LEDGER_ENTRIES.DESCRIPTION);

        for (Leg leg : legs) {
            insert = insert.values(
                    txRef,
                    idempotencyKey,
                    dealUuid,
                    leg.accountId().value(),
                    leg.entryType().name(),
                    leg.debitNano(),
                    leg.creditNano(),
                    description);
        }
        insert.execute();
    }

    /**
     * Returns all entries for a deal, ordered by created_at DESC.
     */
    public @NonNull List<LedgerEntry> findByDealId(@NonNull DealId dealId) {
        return dsl.selectFrom(LEDGER_ENTRIES)
                .where(LEDGER_ENTRIES.DEAL_ID.eq(dealId.value()))
                .orderBy(LEDGER_ENTRIES.CREATED_AT.desc(), LEDGER_ENTRIES.ID.desc())
                .fetch(this::mapEntry);
    }

    /**
     * Returns paginated entries for an account using cursor-based pagination.
     * Cursor is the entry ID; entries are ordered by ID DESC (newest first).
     */
    public @NonNull CursorPage<LedgerEntry> findByAccountId(
            @NonNull AccountId accountId,
            @Nullable String cursor,
            int limit) {

        var query = dsl.selectFrom(LEDGER_ENTRIES)
                .where(LEDGER_ENTRIES.ACCOUNT_ID.eq(accountId.value()));

        if (cursor != null) {
            long cursorId = Long.parseLong(cursor);
            query = query.and(LEDGER_ENTRIES.ID.lt(cursorId));
        }

        List<LedgerEntry> items = query
                .orderBy(LEDGER_ENTRIES.ID.desc())
                .limit(limit + 1)
                .fetch(this::mapEntry);

        if (items.size() > limit) {
            List<LedgerEntry> page = items.subList(0, limit);
            String nextCursor = String.valueOf(page.getLast().id());
            return new CursorPage<>(new ArrayList<>(page), nextCursor);
        }
        return new CursorPage<>(items, null);
    }

    private LedgerEntry mapEntry(Record r) {
        UUID dealUuid = r.get(LEDGER_ENTRIES.DEAL_ID);
        OffsetDateTime createdAt = r.get(LEDGER_ENTRIES.CREATED_AT);
        return new LedgerEntry(
                r.get(LEDGER_ENTRIES.ID),
                dealUuid != null ? DealId.of(dealUuid) : null,
                new AccountId(r.get(LEDGER_ENTRIES.ACCOUNT_ID)),
                EntryType.valueOf(r.get(LEDGER_ENTRIES.ENTRY_TYPE)),
                r.get(LEDGER_ENTRIES.DEBIT_NANO),
                r.get(LEDGER_ENTRIES.CREDIT_NANO),
                r.get(LEDGER_ENTRIES.IDEMPOTENCY_KEY),
                r.get(LEDGER_ENTRIES.TX_REF),
                r.get(LEDGER_ENTRIES.DESCRIPTION),
                createdAt != null ? createdAt.toInstant() : Instant.now());
    }
}

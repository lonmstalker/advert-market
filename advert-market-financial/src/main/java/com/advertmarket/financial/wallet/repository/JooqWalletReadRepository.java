package com.advertmarket.financial.wallet.repository;

import static com.advertmarket.db.generated.tables.Deals.DEALS;
import static com.advertmarket.db.generated.tables.LedgerEntries.LEDGER_ENTRIES;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.sum;

import com.advertmarket.db.generated.tables.records.LedgerEntriesRecord;
import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.ledger.mapper.LedgerEntryMapper;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * Provides jOOQ-backed wallet read model queries.
 */
@Repository
@RequiredArgsConstructor
public class JooqWalletReadRepository implements WalletReadRepository {

    private static final String EXTERNAL_TON_ACCOUNT_ID =
            AccountId.externalTon().value();
    private static final String ESCROW_ACCOUNT_PREFIX = "ESCROW:%";

    private static final Set<String> ADVERTISER_TX_ENTRY_TYPES = Set.of(
            EntryType.ESCROW_DEPOSIT.name(),
            EntryType.PARTIAL_DEPOSIT.name(),
            EntryType.ESCROW_REFUND.name(),
            EntryType.PARTIAL_REFUND.name(),
            EntryType.OVERPAYMENT_REFUND.name(),
            EntryType.LATE_DEPOSIT_REFUND.name());

    private static final Set<String> ADVERTISER_SPEND_ENTRY_TYPES = Set.of(
            EntryType.ESCROW_DEPOSIT.name(),
            EntryType.PARTIAL_DEPOSIT.name());

    private final DSLContext dsl;
    private final LedgerEntryMapper ledgerEntryMapper;

    @Override
    public long sumOwnerTotalEarned(@NonNull UserId userId) {
        var ownerAccountId = AccountId.ownerPending(userId).value();
        Long total = dsl.select(coalesce(sum(LEDGER_ENTRIES.CREDIT_NANO), 0L))
                .from(LEDGER_ENTRIES)
                .where(LEDGER_ENTRIES.ACCOUNT_ID.eq(ownerAccountId))
                .and(LEDGER_ENTRIES.ENTRY_TYPE.eq(EntryType.OWNER_PAYOUT.name()))
                .fetchSingle(0, Long.class);
        return total == null ? 0L : total;
    }

    @Override
    public long sumAdvertiserSpent(@NonNull UserId userId) {
        Long total = dsl.select(coalesce(sum(LEDGER_ENTRIES.DEBIT_NANO), 0L))
                .from(LEDGER_ENTRIES)
                .where(LEDGER_ENTRIES.ACCOUNT_ID.eq(EXTERNAL_TON_ACCOUNT_ID))
                .and(LEDGER_ENTRIES.ENTRY_TYPE.in(ADVERTISER_SPEND_ENTRY_TYPES))
                .and(advertiserDealCondition(userId))
                .fetchSingle(0, Long.class);
        return total == null ? 0L : total;
    }

    @Override
    public long sumAdvertiserActiveEscrow(@NonNull UserId userId) {
        Long total = dsl.select(coalesce(
                        sum(LEDGER_ENTRIES.CREDIT_NANO.minus(LEDGER_ENTRIES.DEBIT_NANO)),
                        0L))
                .from(LEDGER_ENTRIES)
                .where(LEDGER_ENTRIES.ACCOUNT_ID.like(ESCROW_ACCOUNT_PREFIX))
                .and(advertiserDealCondition(userId))
                .fetchSingle(0, Long.class);
        return total == null ? 0L : total;
    }

    @Override
    public @NonNull CursorPage<LedgerEntry> findUserTransactions(
            @NonNull UserId userId,
            @Nullable String cursor,
            int limit) {
        var ownerAccountId = AccountId.ownerPending(userId).value();
        Condition ownerTransactions = LEDGER_ENTRIES.ACCOUNT_ID.eq(ownerAccountId);
        Condition advertiserTransactions = LEDGER_ENTRIES.ACCOUNT_ID
                .eq(EXTERNAL_TON_ACCOUNT_ID)
                .and(LEDGER_ENTRIES.ENTRY_TYPE.in(ADVERTISER_TX_ENTRY_TYPES))
                .and(advertiserDealCondition(userId));

        var query = dsl.selectFrom(LEDGER_ENTRIES)
                .where(ownerTransactions.or(advertiserTransactions));
        if (cursor != null) {
            long cursorId = Long.parseLong(cursor);
            query = query.and(LEDGER_ENTRIES.ID.lt(cursorId));
        }

        List<LedgerEntry> items = query
                .orderBy(LEDGER_ENTRIES.ID.desc())
                .limit(limit + 1)
                .fetchInto(LedgerEntriesRecord.class)
                .stream()
                .map(ledgerEntryMapper::toEntry)
                .toList();

        if (items.size() > limit) {
            List<LedgerEntry> page = items.subList(0, limit);
            String nextCursor = String.valueOf(page.getLast().id());
            return new CursorPage<>(new ArrayList<>(page), nextCursor);
        }
        return new CursorPage<>(items, null);
    }

    private static Condition advertiserDealCondition(
            @NonNull UserId userId) {
        return exists(selectOne()
                .from(DEALS)
                .where(DEALS.ID.eq(LEDGER_ENTRIES.DEAL_ID))
                .and(DEALS.ADVERTISER_ID.eq(userId.value())));
    }
}

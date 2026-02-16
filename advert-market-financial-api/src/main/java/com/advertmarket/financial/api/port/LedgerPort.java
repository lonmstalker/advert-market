package com.advertmarket.financial.api.port;

import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.pagination.CursorPage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Port for double-entry ledger operations.
 */
public interface LedgerPort {

    /**
     * Records a balanced double-entry transfer.
     *
     * @param request transfer command with balanced legs
     * @return transaction reference UUID grouping the entries
     * @throws com.advertmarket.shared.exception.DomainException
     *         with {@code INSUFFICIENT_BALANCE} or {@code LEDGER_INCONSISTENCY}
     */
    @NonNull UUID transfer(@NonNull TransferRequest request);

    /**
     * Returns the current balance in nanoTON for an account (cache + DB fallback).
     * May be negative for contra accounts (EXTERNAL_TON, NETWORK_FEES, DUST_WRITEOFF).
     *
     * @param accountId the account to query
     * @return balance in nanoTON
     */
    long getBalance(@NonNull AccountId accountId);

    /**
     * Returns all ledger entries for a deal, ordered by created_at DESC.
     *
     * @param dealId the deal identifier
     * @return entries list (may be empty)
     */
    @NonNull List<LedgerEntry> getEntriesByDeal(@NonNull DealId dealId);

    /**
     * Returns paginated ledger entries for an account.
     *
     * @param accountId the account to query
     * @param cursor opaque cursor from previous page (null for first page)
     * @param limit max items per page
     * @return cursor page of entries
     */
    @NonNull CursorPage<LedgerEntry> getEntriesByAccount(
            @NonNull AccountId accountId,
            @Nullable String cursor,
            int limit);

    /**
     * Returns total debits for an account and entry type since the given instant.
     *
     * @param accountId account to aggregate
     * @param entryType business operation type
     * @param since inclusive lower bound for created_at
     * @return total debit amount in nanoTON
     */
    long sumDebitsSince(
            @NonNull AccountId accountId,
            @NonNull EntryType entryType,
            @NonNull Instant since);
}

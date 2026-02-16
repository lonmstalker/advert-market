package com.advertmarket.financial.api.port;

import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.api.model.WalletSummary;
import com.advertmarket.financial.api.model.WithdrawalResponse;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Port for user wallet operations.
 */
public interface WalletPort {

    /**
     * Returns aggregated wallet balances for a user.
     *
     * @param userId the user to query
     * @return wallet summary with pending, available, and lifetime balances
     */
    @NonNull WalletSummary getSummary(@NonNull UserId userId);

    /**
     * Returns paginated transaction history for a user's available balance account.
     *
     * @param userId the user to query
     * @param cursor opaque cursor from previous page (null for first page)
     * @param limit  max items per page
     * @return cursor page of ledger entries
     */
    @NonNull CursorPage<LedgerEntry> getTransactions(
            @NonNull UserId userId,
            @Nullable String cursor,
            int limit);

    /**
     * Initiates a withdrawal from the user's available balance.
     *
     * @param userId         the user requesting withdrawal
     * @param amountNano     amount in nanoTON
     * @param idempotencyKey client-provided idempotency key
     * @return withdrawal response with ID and status
     */
    @NonNull WithdrawalResponse withdraw(
            @NonNull UserId userId,
            long amountNano,
            @NonNull String idempotencyKey);
}

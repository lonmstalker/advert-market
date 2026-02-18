package com.advertmarket.financial.wallet.repository;

import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Read-model queries for wallet projections.
 */
public interface WalletReadRepository {

    /**
     * Returns lifetime owner earnings credited to OWNER_PENDING.
     */
    long sumOwnerTotalEarned(@NonNull UserId userId);

    /**
     * Returns advertiser spend total from confirmed deal deposits.
     */
    long sumAdvertiserSpent(@NonNull UserId userId);

    /**
     * Returns current advertiser escrow balance for active deals.
     */
    long sumAdvertiserActiveEscrow(@NonNull UserId userId);

    /**
     * Returns paginated user transactions across supported wallet roles.
     */
    @NonNull CursorPage<LedgerEntry> findUserTransactions(
            @NonNull UserId userId,
            @Nullable String cursor,
            int limit);
}

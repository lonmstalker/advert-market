package com.advertmarket.financial.wallet.service;

import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.api.model.WalletSummary;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.WalletPort;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implements user wallet queries via the ledger.
 *
 * <p>NOT {@code @Component} — wired via
 * {@link com.advertmarket.financial.wallet.config.WalletConfig}.
 */
@RequiredArgsConstructor
public class WalletService implements WalletPort {

    private final LedgerPort ledgerPort;

    @Override
    public @NonNull WalletSummary getSummary(@NonNull UserId userId) {
        var ownerPendingAccount = AccountId.ownerPending(userId);
        long availableBalance = ledgerPort.getBalance(ownerPendingAccount);

        return new WalletSummary(0L, availableBalance, 0L);
    }

    @Override
    public @NonNull CursorPage<LedgerEntry> getTransactions(
            @NonNull UserId userId,
            @Nullable String cursor,
            int limit) {
        var ownerPendingAccount = AccountId.ownerPending(userId);
        return ledgerPort.getEntriesByAccount(ownerPendingAccount, cursor, limit);
    }
}

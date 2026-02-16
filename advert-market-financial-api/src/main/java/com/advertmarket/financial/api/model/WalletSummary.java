package com.advertmarket.financial.api.model;

/**
 * Aggregated wallet balances for a user.
 *
 * @param pendingBalanceNano     escrowed amount in nanoTON (funds in active deals)
 * @param availableBalanceNano   withdrawable amount in nanoTON (OWNER_PENDING account)
 * @param totalEarnedNano        lifetime earnings in nanoTON
 */
public record WalletSummary(
        long pendingBalanceNano,
        long availableBalanceNano,
        long totalEarnedNano) {
}

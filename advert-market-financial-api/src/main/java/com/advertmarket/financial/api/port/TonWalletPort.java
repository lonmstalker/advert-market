package com.advertmarket.financial.api.port;

import com.advertmarket.financial.api.model.DepositAddressInfo;
import com.advertmarket.shared.model.DealId;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Business-level wallet operations for TON blockchain.
 *
 * <p>Generates per-deal deposit addresses and submits signed transactions.
 */
public interface TonWalletPort {

    /** Generates a unique deposit address for a deal. */
    @NonNull DepositAddressInfo generateDepositAddress(@NonNull DealId dealId);

    /** Submits a signed TON transaction and returns the TX hash. */
    @NonNull String submitTransaction(
            int subwalletId, @NonNull String destinationAddress, long amountNano);
}

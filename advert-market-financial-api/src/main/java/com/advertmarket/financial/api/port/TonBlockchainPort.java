package com.advertmarket.financial.api.port;

import com.advertmarket.financial.api.model.TonTransactionInfo;
import com.advertmarket.financial.api.model.TonOutboundTransferInfo;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Low-level abstraction over TON blockchain API operations.
 *
 * <p>Implementations wrap external APIs (e.g. TON Center) and handle
 * error translation, metrics, and resilience patterns.
 */
public interface TonBlockchainPort {

    /** Fetches recent transactions for an address. */
    @NonNull List<TonTransactionInfo> getTransactions(@NonNull String address, int limit);

    /**
     * Fetches recent outgoing transfers for a wallet address.
     *
     * <p>Default implementation returns empty list so legacy
     * adapters remain source-compatible.
     */
    default @NonNull List<TonOutboundTransferInfo>
            getOutgoingTransfers(@NonNull String address, int limit) {
        return List.of();
    }

    /** Sends a serialized BoC (bag-of-cells) and returns the TX hash. */
    @NonNull String sendBoc(@NonNull String base64Boc);

    /** Returns the latest masterchain block seqno. */
    long getMasterchainSeqno();

    /** Returns address balance in nanoTON. */
    long getAddressBalance(@NonNull String address);

    /** Returns the current seqno of a wallet contract. */
    long getSeqno(@NonNull String address);

    /** Estimates transaction fee in nanoTON. */
    long estimateFee(@NonNull String address, @NonNull String base64Body);
}

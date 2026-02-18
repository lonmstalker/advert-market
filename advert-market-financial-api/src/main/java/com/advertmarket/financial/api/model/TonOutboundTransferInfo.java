package com.advertmarket.financial.api.model;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Outgoing TON transfer extracted from transaction out messages.
 *
 * @param txHash      transaction hash (hex/base64 hash from TON Center)
 * @param lt          logical time
 * @param fromAddress sender wallet address (may be missing in provider payload)
 * @param toAddress   transfer recipient address
 * @param amountNano  transfer amount in nanoTON
 * @param feeNano     transaction fee in nanoTON
 * @param utime       Unix timestamp of the transaction
 */
public record TonOutboundTransferInfo(
        @NonNull String txHash,
        long lt,
        @Nullable String fromAddress,
        @NonNull String toAddress,
        long amountNano,
        long feeNano,
        long utime) {
}

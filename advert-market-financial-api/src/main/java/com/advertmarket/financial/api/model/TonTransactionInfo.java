package com.advertmarket.financial.api.model;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Blockchain transaction information from TON Center API.
 *
 * @param txHash      transaction hash (hex)
 * @param lt          logical time
 * @param fromAddress sender address
 * @param toAddress   recipient address
 * @param amountNano  transfer amount in nanoTON
 * @param feeNano     transaction fee in nanoTON
 * @param utime       Unix timestamp of the transaction
 */
public record TonTransactionInfo(
        @NonNull String txHash,
        long lt,
        @Nullable String fromAddress,
        @NonNull String toAddress,
        long amountNano,
        long feeNano,
        long utime) {
}

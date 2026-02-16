package com.advertmarket.financial.api.model;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Deal deposit projection used by web/API consumers.
 *
 * @param escrowAddress escrow address for incoming TON
 * @param amountNano expected amount in nanoTON (string for JS safety)
 * @param dealId deal identifier as string
 * @param status current deposit status
 * @param currentConfirmations current confirmation count
 * @param requiredConfirmations required confirmation count
 * @param receivedAmountNano received amount in nanoTON, nullable until detected
 * @param txHash blockchain hash, nullable until detected
 * @param expiresAt ISO timestamp when deposit window expires, nullable
 */
public record DepositInfo(
        @NonNull String escrowAddress,
        @NonNull String amountNano,
        @NonNull String dealId,
        @NonNull DepositStatus status,
        @Nullable Integer currentConfirmations,
        @Nullable Integer requiredConfirmations,
        @Nullable String receivedAmountNano,
        @Nullable String txHash,
        @Nullable String expiresAt) {
}

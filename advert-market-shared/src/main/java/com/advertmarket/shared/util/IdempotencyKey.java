package com.advertmarket.shared.util;

import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.UserId;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Idempotency key for financial ledger operations.
 *
 * <p>Each key pattern ensures exactly-once semantics
 * for the corresponding operation type.
 */
public record IdempotencyKey(@NonNull String value) {

    /**
     * Creates an idempotency key.
     *
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is blank
     */
    public IdempotencyKey {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "IdempotencyKey must not be blank");
        }
    }

    /**
     * Key for escrow deposit: {@code deposit:{txHash}}.
     *
     * @param txHash the blockchain transaction hash
     * @return idempotency key
     */
    public static @NonNull IdempotencyKey deposit(
            @NonNull String txHash) {
        requireNotBlank(txHash, "txHash");
        return new IdempotencyKey("deposit:" + txHash);
    }

    /**
     * Key for partial deposit:
     * {@code partial-deposit:{dealId}:{txHash}}.
     *
     * @param dealId the deal identifier
     * @param txHash the blockchain transaction hash
     * @return idempotency key
     */
    public static @NonNull IdempotencyKey partialDeposit(
            @NonNull DealId dealId,
            @NonNull String txHash) {
        requireNotBlank(txHash, "txHash");
        return new IdempotencyKey(
                "partial-deposit:" + dealId.value()
                        + ":" + txHash);
    }

    /**
     * Key for promoting partial deposits:
     * {@code promote:{dealId}}.
     *
     * @param dealId the deal identifier
     * @return idempotency key
     */
    public static @NonNull IdempotencyKey promote(
            @NonNull DealId dealId) {
        return new IdempotencyKey(
                "promote:" + dealId.value());
    }

    /**
     * Key for escrow release: {@code release:{dealId}}.
     *
     * @param dealId the deal identifier
     * @return idempotency key
     */
    public static @NonNull IdempotencyKey release(
            @NonNull DealId dealId) {
        return new IdempotencyKey(
                "release:" + dealId.value());
    }

    /**
     * Key for escrow refund: {@code refund:{dealId}}.
     *
     * @param dealId the deal identifier
     * @return idempotency key
     */
    public static @NonNull IdempotencyKey refund(
            @NonNull DealId dealId) {
        return new IdempotencyKey(
                "refund:" + dealId.value());
    }

    /**
     * Key for partial refund:
     * {@code partial-refund:{dealId}}.
     *
     * @param dealId the deal identifier
     * @return idempotency key
     */
    public static @NonNull IdempotencyKey partialRefund(
            @NonNull DealId dealId) {
        return new IdempotencyKey(
                "partial-refund:" + dealId.value());
    }

    /**
     * Key for overpayment refund:
     * {@code overpayment-refund:{dealId}:{txHash}}.
     *
     * @param dealId the deal identifier
     * @param txHash the blockchain transaction hash
     * @return idempotency key
     */
    public static @NonNull IdempotencyKey overpaymentRefund(
            @NonNull DealId dealId,
            @NonNull String txHash) {
        requireNotBlank(txHash, "txHash");
        return new IdempotencyKey(
                "overpayment-refund:" + dealId.value()
                        + ":" + txHash);
    }

    /**
     * Key for late deposit refund:
     * {@code late-deposit-refund:{dealId}:{txHash}}.
     *
     * @param dealId the deal identifier
     * @param txHash the blockchain transaction hash
     * @return idempotency key
     */
    public static @NonNull IdempotencyKey lateDepositRefund(
            @NonNull DealId dealId,
            @NonNull String txHash) {
        requireNotBlank(txHash, "txHash");
        return new IdempotencyKey(
                "late-deposit-refund:" + dealId.value()
                        + ":" + txHash);
    }

    /**
     * Key for commission sweep:
     * {@code sweep:{date}:{accountId}}.
     *
     * @param date sweep date (e.g., "2026-01-15")
     * @param accountId the commission account
     * @return idempotency key
     */
    public static @NonNull IdempotencyKey sweep(
            @NonNull String date,
            @NonNull AccountId accountId) {
        requireNotBlank(date, "date");
        return new IdempotencyKey(
                "sweep:" + date + ":" + accountId.value());
    }

    /**
     * Key for owner withdrawal:
     * {@code withdrawal:{userId}:{timestamp}}.
     *
     * @param userId the owner's user identifier
     * @param timestamp withdrawal timestamp
     * @return idempotency key
     */
    public static @NonNull IdempotencyKey withdrawal(
            @NonNull UserId userId,
            @NonNull String timestamp) {
        requireNotBlank(timestamp, "timestamp");
        return new IdempotencyKey(
                "withdrawal:" + userId.value()
                        + ":" + timestamp);
    }

    /**
     * Key for network fee: {@code fee:{txHash}}.
     *
     * @param txHash the blockchain transaction hash
     * @return idempotency key
     */
    public static @NonNull IdempotencyKey fee(
            @NonNull String txHash) {
        requireNotBlank(txHash, "txHash");
        return new IdempotencyKey("fee:" + txHash);
    }

    /**
     * Key for reversal: {@code reversal:{originalTxRef}}.
     *
     * @param originalTxRef the original transaction reference
     * @return idempotency key
     */
    public static @NonNull IdempotencyKey reversal(
            @NonNull String originalTxRef) {
        requireNotBlank(originalTxRef, "originalTxRef");
        return new IdempotencyKey(
                "reversal:" + originalTxRef);
    }

    private static void requireNotBlank(
            String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " must not be blank");
        }
    }

    @Override
    public @NonNull String toString() {
        return value;
    }
}

package com.advertmarket.financial.api.model;

import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.util.IdempotencyKey;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Command to record a balanced double-entry transfer.
 *
 * <p>All legs must satisfy the accounting equation:
 * {@code SUM(debit) == SUM(credit)}.
 *
 * @param dealId optional deal reference
 * @param idempotencyKey exactly-once guard
 * @param legs at least 2 non-zero legs
 * @param description optional human-readable description
 */
public record TransferRequest(
        @Nullable DealId dealId,
        @NonNull IdempotencyKey idempotencyKey,
        @NonNull List<Leg> legs,
        @Nullable String description) {

    /** Validates non-null fields and ensures at least 2 legs (defensive copy). */
    public TransferRequest {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(legs, "legs");
        legs = List.copyOf(legs);
        if (legs.size() < 2) {
            throw new IllegalArgumentException(
                    "Transfer requires at least 2 legs, got: "
                            + legs.size());
        }
    }

    /**
     * Creates a balanced transfer request, validating SUM(debit) == SUM(credit).
     *
     * @throws IllegalArgumentException if the transfer is unbalanced
     */
    public static @NonNull TransferRequest balanced(
            @Nullable DealId dealId,
            @NonNull IdempotencyKey idempotencyKey,
            @NonNull List<Leg> legs,
            @Nullable String description) {

        var request = new TransferRequest(
                dealId, idempotencyKey, legs, description);
        validateBalance(request.legs());
        return request;
    }

    /**
     * Filters out zero-amount legs and re-validates.
     *
     * @return new request with only non-zero legs
     * @throws IllegalArgumentException if fewer than 2 non-zero legs
     *         or transfer becomes unbalanced
     */
    public @NonNull TransferRequest withoutZeroLegs() {
        List<Leg> nonZero = legs.stream()
                .filter(leg -> !leg.amount().isZero())
                .toList();
        if (nonZero.size() == legs.size()) {
            return this;
        }
        return balanced(dealId, idempotencyKey, nonZero, description);
    }

    private static void validateBalance(List<Leg> legs) {
        long totalDebit = 0;
        long totalCredit = 0;
        for (Leg leg : legs) {
            if (leg.isDebit()) {
                totalDebit = Math.addExact(totalDebit,
                        leg.amount().nanoTon());
            } else {
                totalCredit = Math.addExact(totalCredit,
                        leg.amount().nanoTon());
            }
        }
        if (totalDebit != totalCredit) {
            throw new IllegalArgumentException(
                    "Transfer is unbalanced: debit="
                            + Money.ofNano(totalDebit)
                            + " credit="
                            + Money.ofNano(totalCredit));
        }
    }
}

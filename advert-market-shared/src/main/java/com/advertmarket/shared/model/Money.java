package com.advertmarket.shared.model;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Monetary amount in nanoTON (1 TON = 10^9 nanoTON).
 *
 * <p>All amounts are non-negative. Arithmetic operations use
 * {@link Math#addExact} and {@link Math#multiplyExact} for
 * overflow protection.
 */
public record Money(long nanoTon) implements Comparable<Money> {

    /** Number of nanoTON in one TON. */
    public static final long NANO_PER_TON = 1_000_000_000L;

    /**
     * Creates a money amount.
     *
     * @throws IllegalArgumentException if nanoTon is negative
     */
    public Money {
        if (nanoTon < 0) {
            throw new IllegalArgumentException(
                    "nanoTon must be >= 0, got: " + nanoTon);
        }
    }

    /** Returns zero amount. */
    public static @NonNull Money zero() {
        return new Money(0);
    }

    /**
     * Creates money from nanoTON.
     *
     * @param nanoTon amount in nanoTON
     * @return money instance
     */
    public static @NonNull Money ofNano(long nanoTon) {
        return new Money(nanoTon);
    }

    /**
     * Creates money from whole TON.
     *
     * @param ton amount in whole TON
     * @return money instance
     */
    public static @NonNull Money ofTon(long ton) {
        return new Money(Math.multiplyExact(ton, NANO_PER_TON));
    }

    /**
     * Adds another amount to this one.
     *
     * @param other amount to add
     * @return sum of the two amounts
     * @throws ArithmeticException on overflow
     */
    public @NonNull Money add(@NonNull Money other) {
        return new Money(
                Math.addExact(this.nanoTon, other.nanoTon));
    }

    /**
     * Subtracts another amount from this one.
     *
     * @param other amount to subtract
     * @return difference
     * @throws IllegalArgumentException if result is negative
     */
    public @NonNull Money subtract(@NonNull Money other) {
        return new Money(
                Math.subtractExact(this.nanoTon, other.nanoTon));
    }

    /**
     * Multiplies this amount by a non-negative factor.
     *
     * @param factor non-negative multiplicand
     * @return product
     * @throws ArithmeticException on overflow
     * @throws IllegalArgumentException if factor is negative
     */
    public @NonNull Money multiply(long factor) {
        if (factor < 0) {
            throw new IllegalArgumentException(
                    "factor must be >= 0, got: " + factor);
        }
        return new Money(
                Math.multiplyExact(this.nanoTon, factor));
    }

    /** Returns {@code true} if this amount is zero. */
    public boolean isZero() {
        return nanoTon == 0;
    }

    /** Returns {@code true} if this exceeds the other. */
    public boolean isGreaterThan(@NonNull Money other) {
        return this.nanoTon > other.nanoTon;
    }

    /** Returns {@code true} if this is less than the other. */
    public boolean isLessThan(@NonNull Money other) {
        return this.nanoTon < other.nanoTon;
    }

    @Override
    public int compareTo(@NonNull Money other) {
        return Long.compare(this.nanoTon, other.nanoTon);
    }

    @Override
    public @NonNull String toString() {
        long wholeTon = nanoTon / NANO_PER_TON;
        long fraction = nanoTon % NANO_PER_TON;
        return String.format("%d.%09d TON", wholeTon, fraction);
    }
}

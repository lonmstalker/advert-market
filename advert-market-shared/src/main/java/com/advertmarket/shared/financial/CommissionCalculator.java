package com.advertmarket.shared.financial;

import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.model.Money;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Stateless utility for platform commission calculation.
 *
 * <p>Uses integer arithmetic on nanoTON values to avoid
 * floating-point rounding errors. Commission rate is expressed
 * in basis points (1 bp = 0.01%).
 */
public final class CommissionCalculator {

    /** Maximum allowed commission rate: 50% (5000 basis points). */
    public static final int MAX_RATE_BP = 5_000;

    private static final int BP_DIVISOR = 10_000;

    private CommissionCalculator() {
    }

    /**
     * Calculates commission and owner payout.
     *
     * @param amount deal amount (must be positive)
     * @param rateBp commission rate in basis points [0, 5000]
     * @return commission result with commission and owner payout
     * @throws DomainException if inputs are invalid
     */
    public static @NonNull CommissionResult calculate(
            @NonNull Money amount, int rateBp) {
        if (amount.isZero()) {
            throw new DomainException(
                    ErrorCodes.VALIDATION_FAILED,
                    "Amount must be positive");
        }
        if (rateBp < 0 || rateBp > MAX_RATE_BP) {
            throw new DomainException(
                    ErrorCodes.VALIDATION_FAILED,
                    "Rate must be in [0, " + MAX_RATE_BP
                            + "] bp, got: " + rateBp);
        }

        try {
            long commissionNano = Math.multiplyExact(
                    amount.nanoTon(), rateBp) / BP_DIVISOR;
            Money commission = Money.ofNano(commissionNano);
            Money ownerPayout = amount.subtract(commission);

            return new CommissionResult(commission, ownerPayout);
        } catch (ArithmeticException ex) {
            throw new DomainException(
                    ErrorCodes.COMMISSION_CALCULATION_ERROR,
                    "Commission calculation overflow for amount="
                            + amount + " rateBp=" + rateBp);
        }
    }
}

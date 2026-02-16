package com.advertmarket.financial.escrow.service;

/**
 * Pure function: validates received deposit amount against expected amount.
 *
 * <p>Supports configurable tolerance to account for network fees
 * and minor rounding differences in TON transactions.
 */
public final class DepositAmountValidator {

    private DepositAmountValidator() {
    }

    /**
     * Validates received amount against expected amount (exact match).
     *
     * @param expectedNano expected deposit amount in nanoTON
     * @param receivedNano actual received amount in nanoTON
     * @return validation result
     */
    public static Result validate(long expectedNano,
            long receivedNano) {
        return validate(expectedNano, receivedNano, 0L);
    }

    /**
     * Validates received amount against expected with tolerance.
     *
     * <p>Amount within {@code [expected - tolerance, expected + tolerance]}
     * is treated as {@link Result#EXACT_MATCH}.
     *
     * @param expectedNano  expected deposit amount in nanoTON
     * @param receivedNano  actual received amount in nanoTON
     * @param toleranceNano acceptable deviation in nanoTON
     * @return validation result
     */
    public static Result validate(long expectedNano,
            long receivedNano, long toleranceNano) {
        long diff = receivedNano - expectedNano;
        if (Math.abs(diff) <= toleranceNano) {
            return Result.EXACT_MATCH;
        }
        return diff > 0 ? Result.OVERPAYMENT : Result.UNDERPAYMENT;
    }

    /**
     * Returns the excess amount (overpayment), or zero if not over.
     *
     * @param expectedNano expected amount in nanoTON
     * @param receivedNano received amount in nanoTON
     * @return excess in nanoTON, or 0
     */
    public static long excessNano(long expectedNano,
            long receivedNano) {
        return Math.max(0, receivedNano - expectedNano);
    }

    /**
     * Returns the deficit amount (underpayment), or zero if not under.
     *
     * @param expectedNano expected amount in nanoTON
     * @param receivedNano received amount in nanoTON
     * @return deficit in nanoTON, or 0
     */
    public static long deficitNano(long expectedNano,
            long receivedNano) {
        return Math.max(0, expectedNano - receivedNano);
    }

    /** Deposit amount validation result. */
    public enum Result {
        EXACT_MATCH,
        OVERPAYMENT,
        UNDERPAYMENT
    }
}

package com.advertmarket.financial.escrow.service;

/**
 * Pure function: validates received deposit amount against expected amount.
 */
public final class DepositAmountValidator {

    private DepositAmountValidator() {
    }

    /**
     * Validates received amount against expected amount.
     *
     * @param expectedNano expected deposit amount in nanoTON
     * @param receivedNano actual received amount in nanoTON
     * @return validation result
     */
    public static Result validate(long expectedNano, long receivedNano) {
        if (receivedNano == expectedNano) {
            return Result.EXACT_MATCH;
        }
        if (receivedNano > expectedNano) {
            return Result.OVERPAYMENT;
        }
        return Result.UNDERPAYMENT;
    }

    /** Deposit amount validation result. */
    public enum Result {
        EXACT_MATCH,
        OVERPAYMENT,
        UNDERPAYMENT
    }
}

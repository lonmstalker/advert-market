package com.advertmarket.financial.escrow.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DepositAmountValidator — validates deposit amounts")
class DepositAmountValidatorTest {

    @Test
    @DisplayName("Should return EXACT_MATCH when amounts are equal")
    void exactMatch() {
        assertThat(DepositAmountValidator.validate(100L, 100L))
                .isEqualTo(DepositAmountValidator.Result.EXACT_MATCH);
    }

    @Test
    @DisplayName("Should return OVERPAYMENT when received exceeds expected")
    void overpayment() {
        assertThat(DepositAmountValidator.validate(100L, 200L))
                .isEqualTo(DepositAmountValidator.Result.OVERPAYMENT);
    }

    @Test
    @DisplayName("Should return UNDERPAYMENT when received is less than expected")
    void underpayment() {
        assertThat(DepositAmountValidator.validate(100L, 50L))
                .isEqualTo(DepositAmountValidator.Result.UNDERPAYMENT);
    }

    @Test
    @DisplayName("Should handle zero expected and received")
    void zeroAmounts() {
        assertThat(DepositAmountValidator.validate(0L, 0L))
                .isEqualTo(DepositAmountValidator.Result.EXACT_MATCH);
    }

    @Test
    @DisplayName("Should return OVERPAYMENT for even 1 nanoTON over")
    void overpaymentByOne() {
        assertThat(DepositAmountValidator.validate(1_000_000_000L, 1_000_000_001L))
                .isEqualTo(DepositAmountValidator.Result.OVERPAYMENT);
    }
}

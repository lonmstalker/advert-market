package com.advertmarket.financial.escrow.service;

import static com.advertmarket.financial.escrow.service.DepositAmountValidator.Result.EXACT_MATCH;
import static com.advertmarket.financial.escrow.service.DepositAmountValidator.Result.OVERPAYMENT;
import static com.advertmarket.financial.escrow.service.DepositAmountValidator.Result.UNDERPAYMENT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DepositAmountValidator — validates deposit amounts")
class DepositAmountValidatorTest {

    private static final long TON = 1_000_000_000L;
    private static final long DEFAULT_TOLERANCE = 1_000_000L;

    @Nested
    @DisplayName("validate without tolerance (legacy)")
    class NoTolerance {

        @Test
        @DisplayName("Should return EXACT_MATCH when amounts are equal")
        void exactMatch() {
            assertThat(DepositAmountValidator.validate(100L, 100L))
                    .isEqualTo(EXACT_MATCH);
        }

        @Test
        @DisplayName("Should return OVERPAYMENT when received exceeds")
        void overpayment() {
            assertThat(DepositAmountValidator.validate(100L, 200L))
                    .isEqualTo(OVERPAYMENT);
        }

        @Test
        @DisplayName("Should return UNDERPAYMENT when received is less")
        void underpayment() {
            assertThat(DepositAmountValidator.validate(100L, 50L))
                    .isEqualTo(UNDERPAYMENT);
        }

        @Test
        @DisplayName("Should handle zero amounts")
        void zeroAmounts() {
            assertThat(DepositAmountValidator.validate(0L, 0L))
                    .isEqualTo(EXACT_MATCH);
        }
    }

    @Nested
    @DisplayName("validate with tolerance")
    class WithTolerance {

        @Test
        @DisplayName("Should return EXACT_MATCH within tolerance")
        void withinTolerance() {
            long expected = TON;
            long received = TON - DEFAULT_TOLERANCE;
            assertThat(DepositAmountValidator.validate(
                    expected, received, DEFAULT_TOLERANCE))
                    .isEqualTo(EXACT_MATCH);
        }

        @Test
        @DisplayName("Should return EXACT_MATCH at tolerance boundary")
        void atToleranceBoundary() {
            long expected = TON;
            long received = expected - DEFAULT_TOLERANCE;
            assertThat(DepositAmountValidator.validate(
                    expected, received, DEFAULT_TOLERANCE))
                    .isEqualTo(EXACT_MATCH);
        }

        @Test
        @DisplayName("Should return UNDERPAYMENT beyond tolerance")
        void beyondTolerance() {
            long expected = TON;
            long received = expected - DEFAULT_TOLERANCE - 1;
            assertThat(DepositAmountValidator.validate(
                    expected, received, DEFAULT_TOLERANCE))
                    .isEqualTo(UNDERPAYMENT);
        }

        @Test
        @DisplayName("Should return OVERPAYMENT above tolerance")
        void overpaymentAboveTolerance() {
            long expected = TON;
            long received = expected + DEFAULT_TOLERANCE + 1;
            assertThat(DepositAmountValidator.validate(
                    expected, received, DEFAULT_TOLERANCE))
                    .isEqualTo(OVERPAYMENT);
        }

        @Test
        @DisplayName("Should return EXACT_MATCH for slight overpay within tolerance")
        void slightOverpayWithinTolerance() {
            long expected = TON;
            long received = expected + DEFAULT_TOLERANCE;
            assertThat(DepositAmountValidator.validate(
                    expected, received, DEFAULT_TOLERANCE))
                    .isEqualTo(EXACT_MATCH);
        }

        @Test
        @DisplayName("Should return EXACT_MATCH for exact amount with tolerance")
        void exactWithTolerance() {
            assertThat(DepositAmountValidator.validate(
                    TON, TON, DEFAULT_TOLERANCE))
                    .isEqualTo(EXACT_MATCH);
        }

        @Test
        @DisplayName("Should return EXACT_MATCH for zero tolerance exact match")
        void zeroToleranceExact() {
            assertThat(DepositAmountValidator.validate(
                    TON, TON, 0L))
                    .isEqualTo(EXACT_MATCH);
        }

        @Test
        @DisplayName("Should return OVERPAYMENT for 1 nanoTON over zero tolerance")
        void zeroToleranceOverpayment() {
            assertThat(DepositAmountValidator.validate(
                    TON, TON + 1, 0L))
                    .isEqualTo(OVERPAYMENT);
        }
    }

    @Nested
    @DisplayName("excessNano")
    class ExcessNano {

        @Test
        @DisplayName("Should return excess for overpayment")
        void overpaymentExcess() {
            assertThat(DepositAmountValidator.excessNano(
                    TON, TON + 500_000L))
                    .isEqualTo(500_000L);
        }

        @Test
        @DisplayName("Should return zero for exact match")
        void exactMatchExcess() {
            assertThat(DepositAmountValidator.excessNano(TON, TON))
                    .isZero();
        }

        @Test
        @DisplayName("Should return zero for underpayment")
        void underpaymentExcess() {
            assertThat(DepositAmountValidator.excessNano(
                    TON, TON - 100L))
                    .isZero();
        }
    }

    @Nested
    @DisplayName("deficitNano")
    class DeficitNano {

        @Test
        @DisplayName("Should return deficit for underpayment")
        void underpaymentDeficit() {
            assertThat(DepositAmountValidator.deficitNano(
                    TON, TON - 500_000L))
                    .isEqualTo(500_000L);
        }

        @Test
        @DisplayName("Should return zero for exact match")
        void exactMatchDeficit() {
            assertThat(DepositAmountValidator.deficitNano(TON, TON))
                    .isZero();
        }

        @Test
        @DisplayName("Should return zero for overpayment")
        void overpaymentDeficit() {
            assertThat(DepositAmountValidator.deficitNano(
                    TON, TON + 100L))
                    .isZero();
        }
    }
}

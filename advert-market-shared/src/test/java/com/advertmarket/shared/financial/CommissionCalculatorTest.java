package com.advertmarket.shared.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CommissionCalculator")
class CommissionCalculatorTest {

    @Test
    @DisplayName("Calculates 5% commission correctly")
    void calculate_fivePercent_correctResult() {
        Money amount = Money.ofTon(10);
        CommissionResult result =
                CommissionCalculator.calculate(amount, 500);

        assertThat(result.commission())
                .isEqualTo(Money.ofNano(500_000_000L));
        assertThat(result.ownerPayout())
                .isEqualTo(Money.ofNano(9_500_000_000L));
    }

    @Test
    @DisplayName("Zero rate produces zero commission")
    void calculate_zeroRate_zeroCommission() {
        Money amount = Money.ofTon(5);
        CommissionResult result =
                CommissionCalculator.calculate(amount, 0);

        assertThat(result.commission()).isEqualTo(Money.zero());
        assertThat(result.ownerPayout()).isEqualTo(amount);
    }

    @Test
    @DisplayName("Maximum rate (50%) produces correct split")
    void calculate_maxRate_correctSplit() {
        Money amount = Money.ofTon(4);
        CommissionResult result =
                CommissionCalculator.calculate(amount, 5_000);

        assertThat(result.commission())
                .isEqualTo(Money.ofTon(2));
        assertThat(result.ownerPayout())
                .isEqualTo(Money.ofTon(2));
    }

    @Test
    @DisplayName("Commission plus payout equals original amount")
    void calculate_commissionPlusPayoutEqualsAmount() {
        Money amount = Money.ofNano(1_000_000_007L);
        CommissionResult result =
                CommissionCalculator.calculate(amount, 333);

        assertThat(result.commission().add(result.ownerPayout()))
                .isEqualTo(amount);
    }

    @Test
    @DisplayName("Small amount with high rate truncates correctly")
    void calculate_smallAmount_truncatesCorrectly() {
        Money amount = Money.ofNano(99);
        CommissionResult result =
                CommissionCalculator.calculate(amount, 500);

        // 99 * 500 / 10000 = 4 (integer truncation)
        assertThat(result.commission())
                .isEqualTo(Money.ofNano(4));
        assertThat(result.ownerPayout())
                .isEqualTo(Money.ofNano(95));
    }

    @Test
    @DisplayName("1 nanoTon with 1 bp produces zero commission")
    void calculate_minimalAmount_zeroCommission() {
        Money amount = Money.ofNano(1);
        CommissionResult result =
                CommissionCalculator.calculate(amount, 1);

        assertThat(result.commission()).isEqualTo(Money.zero());
        assertThat(result.ownerPayout()).isEqualTo(amount);
    }

    @Test
    @DisplayName("Zero amount throws DomainException")
    void calculate_zeroAmount_throws() {
        assertThatThrownBy(() ->
                CommissionCalculator.calculate(Money.zero(), 500))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Amount must be positive");
    }

    @Test
    @DisplayName("Negative rate throws DomainException")
    void calculate_negativeRate_throws() {
        assertThatThrownBy(() ->
                CommissionCalculator.calculate(
                        Money.ofTon(1), -1))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Rate must be in");
    }

    @Test
    @DisplayName("Rate exceeding maximum throws DomainException")
    void calculate_excessiveRate_throws() {
        assertThatThrownBy(() ->
                CommissionCalculator.calculate(
                        Money.ofTon(1), 5_001))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Rate must be in");
    }

    @Test
    @DisplayName("Large amount does not overflow")
    void calculate_largeAmount_noOverflow() {
        // 900 TON * 5000bp should not overflow
        Money amount = Money.ofTon(900);
        CommissionResult result =
                CommissionCalculator.calculate(amount, 5_000);

        assertThat(result.commission())
                .isEqualTo(Money.ofTon(450));
        assertThat(result.ownerPayout())
                .isEqualTo(Money.ofTon(450));
    }
}

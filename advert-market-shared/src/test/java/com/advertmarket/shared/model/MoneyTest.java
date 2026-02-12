package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Money value object")
class MoneyTest {

    @Test
    @DisplayName("Zero factory returns zero amount")
    void zero_returnsZeroAmount() {
        Money zero = Money.zero();
        assertThat(zero.nanoTon()).isZero();
        assertThat(zero.isZero()).isTrue();
    }

    @Test
    @DisplayName("ofNano creates from nanoTON")
    void ofNano_createsFromNanoTon() {
        Money money = Money.ofNano(1_500_000_000L);
        assertThat(money.nanoTon()).isEqualTo(1_500_000_000L);
    }

    @Test
    @DisplayName("ofTon creates from whole TON")
    void ofTon_createsFromWholeTon() {
        Money money = Money.ofTon(2);
        assertThat(money.nanoTon())
                .isEqualTo(2_000_000_000L);
    }

    @Test
    @DisplayName("Negative nanoTon is rejected")
    void constructor_negativeNanoTon_throws() {
        assertThatThrownBy(() -> Money.ofNano(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nanoTon must be >= 0");
    }

    @Test
    @DisplayName("Add produces correct sum")
    void add_producesCorrectSum() {
        Money a = Money.ofNano(100);
        Money b = Money.ofNano(200);
        assertThat(a.add(b)).isEqualTo(Money.ofNano(300));
    }

    @Test
    @DisplayName("Add overflow throws ArithmeticException")
    void add_overflow_throws() {
        Money max = Money.ofNano(Long.MAX_VALUE);
        assertThatThrownBy(() -> max.add(Money.ofNano(1)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("Subtract produces correct difference")
    void subtract_producesCorrectDifference() {
        Money a = Money.ofNano(300);
        Money b = Money.ofNano(100);
        assertThat(a.subtract(b)).isEqualTo(Money.ofNano(200));
    }

    @Test
    @DisplayName("Subtract to negative throws")
    void subtract_negativeResult_throws() {
        Money a = Money.ofNano(100);
        Money b = Money.ofNano(200);
        assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Multiply by factor produces correct product")
    void multiply_producesCorrectProduct() {
        Money money = Money.ofNano(100);
        assertThat(money.multiply(3))
                .isEqualTo(Money.ofNano(300));
    }

    @Test
    @DisplayName("Multiply by zero produces zero")
    void multiply_byZero_producesZero() {
        Money money = Money.ofTon(1);
        assertThat(money.multiply(0)).isEqualTo(Money.zero());
    }

    @Test
    @DisplayName("Multiply by negative factor throws")
    void multiply_negativeFactor_throws() {
        assertThatThrownBy(() -> Money.ofNano(1).multiply(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("factor must be >= 0");
    }

    @Test
    @DisplayName("isGreaterThan compares correctly")
    void isGreaterThan_comparesCorrectly() {
        Money big = Money.ofNano(200);
        Money small = Money.ofNano(100);
        assertThat(big.isGreaterThan(small)).isTrue();
        assertThat(small.isGreaterThan(big)).isFalse();
        assertThat(big.isGreaterThan(big)).isFalse();
    }

    @Test
    @DisplayName("isLessThan compares correctly")
    void isLessThan_comparesCorrectly() {
        Money big = Money.ofNano(200);
        Money small = Money.ofNano(100);
        assertThat(small.isLessThan(big)).isTrue();
        assertThat(big.isLessThan(small)).isFalse();
    }

    @Test
    @DisplayName("compareTo provides natural ordering")
    void compareTo_providesNaturalOrdering() {
        Money a = Money.ofNano(100);
        Money b = Money.ofNano(200);
        Money c = Money.ofNano(100);
        assertThat(a.compareTo(b)).isNegative();
        assertThat(b.compareTo(a)).isPositive();
        assertThat(a.compareTo(c)).isZero();
    }

    @Test
    @DisplayName("toString formats as TON with 9 decimal places")
    void toString_formatsAsTon() {
        assertThat(Money.ofNano(1_500_000_000L).toString())
                .isEqualTo("1.500000000 TON");
        assertThat(Money.zero().toString())
                .isEqualTo("0.000000000 TON");
        assertThat(Money.ofTon(1).toString())
                .isEqualTo("1.000000000 TON");
        assertThat(Money.ofNano(1).toString())
                .isEqualTo("0.000000001 TON");
    }

    @Test
    @DisplayName("Record equality based on nanoTon")
    void equality_basedOnNanoTon() {
        assertThat(Money.ofNano(100))
                .isEqualTo(Money.ofNano(100));
        assertThat(Money.ofNano(100))
                .isNotEqualTo(Money.ofNano(200));
    }
}

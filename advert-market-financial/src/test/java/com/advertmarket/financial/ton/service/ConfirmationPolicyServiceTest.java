package com.advertmarket.financial.ton.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.financial.config.TonProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConfirmationPolicyService — tiered confirmation policy")
class ConfirmationPolicyServiceTest {

    private static final long NANO_PER_TON = 1_000_000_000L;

    private ConfirmationPolicyService service;

    @BeforeEach
    void setUp() {
        var tiers = List.of(
                new TonProperties.Confirmation.Tier(100L * NANO_PER_TON, 1, false),
                new TonProperties.Confirmation.Tier(1_000L * NANO_PER_TON, 3, false),
                new TonProperties.Confirmation.Tier(Long.MAX_VALUE, 5, true));
        var confirmation = new TonProperties.Confirmation(tiers);
        service = new ConfirmationPolicyService(confirmation);
    }

    @Test
    @DisplayName("Should require 1 confirmation for small deposit (1 TON)")
    void smallDeposit() {
        var result = service.requiredConfirmations(1L * NANO_PER_TON);

        assertThat(result.confirmations()).isEqualTo(1);
        assertThat(result.operatorReview()).isFalse();
    }

    @Test
    @DisplayName("Should require 1 confirmation at exactly 100 TON boundary")
    void exactLowBoundary() {
        var result = service.requiredConfirmations(100L * NANO_PER_TON);

        assertThat(result.confirmations()).isEqualTo(1);
        assertThat(result.operatorReview()).isFalse();
    }

    @Test
    @DisplayName("Should require 3 confirmations just above 100 TON")
    void justAboveLowBoundary() {
        var result = service.requiredConfirmations(100L * NANO_PER_TON + 1);

        assertThat(result.confirmations()).isEqualTo(3);
        assertThat(result.operatorReview()).isFalse();
    }

    @Test
    @DisplayName("Should require 3 confirmations at exactly 1000 TON boundary")
    void exactMidBoundary() {
        var result = service.requiredConfirmations(1_000L * NANO_PER_TON);

        assertThat(result.confirmations()).isEqualTo(3);
        assertThat(result.operatorReview()).isFalse();
    }

    @Test
    @DisplayName("Should require 5 confirmations and operator review above 1000 TON")
    void largeDeposit() {
        var result = service.requiredConfirmations(1_000L * NANO_PER_TON + 1);

        assertThat(result.confirmations()).isEqualTo(5);
        assertThat(result.operatorReview()).isTrue();
    }

    @Test
    @DisplayName("Should handle zero amount with minimum tier")
    void zeroAmount() {
        var result = service.requiredConfirmations(0L);

        assertThat(result.confirmations()).isEqualTo(1);
        assertThat(result.operatorReview()).isFalse();
    }
}

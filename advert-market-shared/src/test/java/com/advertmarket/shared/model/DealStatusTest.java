package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("DealStatus enum")
class DealStatusTest {

    @Test
    @DisplayName("Has exactly 17 states")
    void hasExpectedCount() {
        assertThat(DealStatus.values()).hasSize(17);
    }

    @ParameterizedTest
    @EnumSource(value = DealStatus.class,
            names = {"COMPLETED_RELEASED", "CANCELLED",
                    "REFUNDED", "PARTIALLY_REFUNDED",
                    "EXPIRED"})
    @DisplayName("Terminal states")
    void terminalStates(DealStatus status) {
        assertThat(status.isTerminal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = DealStatus.class,
            names = {"COMPLETED_RELEASED", "CANCELLED",
                    "REFUNDED", "PARTIALLY_REFUNDED",
                    "EXPIRED"},
            mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Non-terminal states")
    void nonTerminalStates(DealStatus status) {
        assertThat(status.isTerminal()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = DealStatus.class,
            names = {"FUNDED", "CREATIVE_SUBMITTED",
                    "CREATIVE_APPROVED", "SCHEDULED",
                    "PUBLISHED", "DELIVERY_VERIFYING",
                    "DISPUTED"})
    @DisplayName("Funded states have escrow")
    void fundedStates(DealStatus status) {
        assertThat(status.isFunded()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = DealStatus.class,
            names = {"FUNDED", "CREATIVE_SUBMITTED",
                    "CREATIVE_APPROVED", "SCHEDULED",
                    "PUBLISHED", "DELIVERY_VERIFYING",
                    "DISPUTED"},
            mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Non-funded states have no escrow")
    void nonFundedStates(DealStatus status) {
        assertThat(status.isFunded()).isFalse();
    }

    @Test
    @DisplayName("requiresRefundOnCancel matches isFunded")
    void requiresRefundOnCancel_matchesFunded() {
        for (DealStatus status : DealStatus.values()) {
            assertThat(status.requiresRefundOnCancel())
                    .as("Status %s", status)
                    .isEqualTo(status.isFunded());
        }
    }
}

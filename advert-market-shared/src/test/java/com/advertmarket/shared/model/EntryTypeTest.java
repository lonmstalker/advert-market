package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EntryType enum")
class EntryTypeTest {

    @Test
    @DisplayName("Has exactly 17 entry types")
    void hasExpectedCount() {
        assertThat(EntryType.values()).hasSize(17);
    }

    @Test
    @DisplayName("Contains all deposit operations")
    void containsDepositOperations() {
        assertThat(EntryType.valueOf("ESCROW_DEPOSIT"))
                .isNotNull();
        assertThat(EntryType.valueOf("PARTIAL_DEPOSIT"))
                .isNotNull();
        assertThat(EntryType.valueOf("PARTIAL_DEPOSIT_PROMOTE"))
                .isNotNull();
    }

    @Test
    @DisplayName("Contains all release operations")
    void containsReleaseOperations() {
        assertThat(EntryType.valueOf("ESCROW_RELEASE"))
                .isNotNull();
        assertThat(EntryType.valueOf("OWNER_PAYOUT"))
                .isNotNull();
        assertThat(EntryType.valueOf("PLATFORM_COMMISSION"))
                .isNotNull();
    }

    @Test
    @DisplayName("Contains all refund operations")
    void containsRefundOperations() {
        assertThat(EntryType.valueOf("ESCROW_REFUND"))
                .isNotNull();
        assertThat(EntryType.valueOf("PARTIAL_REFUND"))
                .isNotNull();
        assertThat(EntryType.valueOf("OVERPAYMENT_REFUND"))
                .isNotNull();
        assertThat(EntryType.valueOf("LATE_DEPOSIT_REFUND"))
                .isNotNull();
    }

    @Test
    @DisplayName("Contains correction operations")
    void containsCorrectionOperations() {
        assertThat(EntryType.valueOf("REVERSAL")).isNotNull();
        assertThat(EntryType.valueOf("FEE_ADJUSTMENT"))
                .isNotNull();
        assertThat(EntryType.valueOf("DUST_WRITEOFF"))
                .isNotNull();
    }
}

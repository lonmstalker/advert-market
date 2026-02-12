package com.advertmarket.financial.api.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReconciliationStartEvent")
class ReconciliationStartEventTest {

    @Test
    @DisplayName("Creates defensive copy of checks list")
    void defensiveCopy_checksList() {
        var mutable = new ArrayList<>(List.of(
                ReconciliationCheck.LEDGER_BALANCE,
                ReconciliationCheck.LEDGER_VS_TON));

        var event = new ReconciliationStartEvent(
                ReconciliationTriggerType.SCHEDULED,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-02T00:00:00Z"),
                mutable);

        mutable.add(ReconciliationCheck.CQRS_PROJECTION);

        assertThat(event.checks()).hasSize(2);
    }

    @Test
    @DisplayName("Checks list is immutable")
    void checksList_isImmutable() {
        var event = new ReconciliationStartEvent(
                ReconciliationTriggerType.MANUAL,
                Instant.now(),
                Instant.now(),
                List.of(ReconciliationCheck.LEDGER_BALANCE));

        assertThatThrownBy(
                () -> event.checks().add(
                        ReconciliationCheck.CQRS_PROJECTION))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }
}

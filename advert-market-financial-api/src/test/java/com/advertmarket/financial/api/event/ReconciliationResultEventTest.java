package com.advertmarket.financial.api.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.shared.event.DomainEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReconciliationResultEvent")
class ReconciliationResultEventTest {

    @Test
    @DisplayName("Implements DomainEvent")
    void implementsDomainEvent() {
        var event = new ReconciliationResultEvent(
                UUID.randomUUID(), Map.of(), Instant.now());
        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    @DisplayName("Creates defensive copy of checks map")
    void defensiveCopy_checksMap() {
        var mutable = new HashMap<ReconciliationCheck,
                ReconciliationCheckResult>();
        mutable.put(ReconciliationCheck.LEDGER_BALANCE,
                new ReconciliationCheckResult(
                        ReconciliationCheckStatus.PASS, Map.of()));

        var event = new ReconciliationResultEvent(
                UUID.randomUUID(), mutable, Instant.now());

        mutable.put(ReconciliationCheck.CQRS_PROJECTION,
                new ReconciliationCheckResult(
                        ReconciliationCheckStatus.FAIL, Map.of()));

        assertThat(event.checks()).hasSize(1);
    }

    @Test
    @DisplayName("Checks map is immutable")
    void checksMap_isImmutable() {
        var event = new ReconciliationResultEvent(
                UUID.randomUUID(),
                Map.of(ReconciliationCheck.LEDGER_BALANCE,
                        new ReconciliationCheckResult(
                                ReconciliationCheckStatus.PASS,
                                Map.of())),
                Instant.now());

        assertThatThrownBy(
                () -> event.checks().put(
                        ReconciliationCheck.CQRS_PROJECTION,
                        new ReconciliationCheckResult(
                                ReconciliationCheckStatus.FAIL,
                                Map.of())))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("All fields accessible")
    void allFieldsAccessible() {
        var triggerId = UUID.randomUUID();
        var completedAt = Instant.parse("2026-02-13T12:00:00Z");
        var checks = Map.of(
                ReconciliationCheck.LEDGER_VS_TON,
                new ReconciliationCheckResult(
                        ReconciliationCheckStatus.PASS,
                        Map.of("matched", true)));

        var event = new ReconciliationResultEvent(
                triggerId, checks, completedAt);

        assertThat(event.triggerId()).isEqualTo(triggerId);
        assertThat(event.completedAt()).isEqualTo(completedAt);
        assertThat(event.checks()).hasSize(1);
    }
}

package com.advertmarket.financial.ton.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.financial.config.UnclaimedPayoutProperties;
import com.advertmarket.financial.ton.repository.UnclaimedPayoutRepository;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("UnclaimedPayoutScheduler")
class UnclaimedPayoutSchedulerTest {

    private UnclaimedPayoutRepository repository;
    private OutboxRepository outboxRepository;
    private DistributedLockPort lockPort;
    private JsonFacade jsonFacade;
    private MetricsFacade metrics;
    private UnclaimedPayoutScheduler scheduler;

    @BeforeEach
    void setUp() {
        repository = mock(UnclaimedPayoutRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        lockPort = mock(DistributedLockPort.class);
        jsonFacade = mock(JsonFacade.class);
        metrics = mock(MetricsFacade.class);

        var properties = new UnclaimedPayoutProperties(
                "0 30 2 * * *",
                100,
                Duration.ofMinutes(10),
                List.of(1, 7, 21, 30),
                30,
                "ru");

        scheduler = new UnclaimedPayoutScheduler(
                repository,
                outboxRepository,
                lockPort,
                jsonFacade,
                metrics,
                properties);
    }

    @Test
    @DisplayName("Should emit reminders for reached thresholds when owner has no TON address")
    void shouldEmitReminderThresholds() {
        final var now = OffsetDateTime.of(
                2026, 2, 18, 10, 0, 0, 0, ZoneOffset.UTC);
        var candidate = new UnclaimedPayoutCandidate(
                UUID.randomUUID(),
                101L,
                2_000_000_000L,
                200_000_000L,
                11,
                Instant.parse("2026-02-11T09:00:00Z"),
                false);

        when(repository.findOpenPayoutCandidates(100)).thenReturn(List.of(candidate));
        when(jsonFacade.toJson(any())).thenReturn("{}");

        scheduler.processAt(now);

        var captor = ArgumentCaptor.forClass(OutboxEntry.class);
        verify(outboxRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(OutboxEntry::idempotencyKey)
                .containsExactlyInAnyOrder(
                        "unclaimed-payout:reminder:%s:%d".formatted(candidate.dealId(), 1),
                        "unclaimed-payout:reminder:%s:%d".formatted(candidate.dealId(), 7));
    }

    @Test
    @DisplayName("Should emit operator review notifications on day 30+")
    void shouldEmitOperatorReviewNotifications() {
        final var now = OffsetDateTime.of(
                2026, 2, 18, 10, 0, 0, 0, ZoneOffset.UTC);
        var candidate = new UnclaimedPayoutCandidate(
                UUID.randomUUID(),
                101L,
                2_000_000_000L,
                200_000_000L,
                11,
                Instant.parse("2026-01-10T09:00:00Z"),
                false);

        when(repository.findOpenPayoutCandidates(100)).thenReturn(List.of(candidate));
        when(repository.findOperatorUserIds()).thenReturn(List.of(9001L, 9002L));
        when(jsonFacade.toJson(any())).thenReturn("{}");

        scheduler.processAt(now);

        var captor = ArgumentCaptor.forClass(OutboxEntry.class);
        verify(outboxRepository, org.mockito.Mockito.times(6)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(OutboxEntry::idempotencyKey)
                .contains(
                        "unclaimed-payout:operator-review:%s:%d:%d"
                                .formatted(candidate.dealId(), 9001L, 30),
                        "unclaimed-payout:operator-review:%s:%d:%d"
                                .formatted(candidate.dealId(), 9002L, 30));
    }

    @Test
    @DisplayName("Should emit EXECUTE_PAYOUT retry command when TON address exists")
    void shouldEmitRetryCommandWhenTonAddressExists() {
        final var now = OffsetDateTime.of(
                2026, 2, 18, 10, 0, 0, 0, ZoneOffset.UTC);
        var candidate = new UnclaimedPayoutCandidate(
                UUID.randomUUID(),
                55L,
                3_000_000_000L,
                300_000_000L,
                21,
                Instant.parse("2026-02-01T09:00:00Z"),
                true);

        when(repository.findOpenPayoutCandidates(100)).thenReturn(List.of(candidate));
        when(jsonFacade.toJson(any())).thenReturn("{}");

        scheduler.processAt(now);

        var captor = ArgumentCaptor.forClass(OutboxEntry.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().idempotencyKey())
                .isEqualTo("unclaimed-payout:retry:%s".formatted(candidate.dealId()));
    }

    @Test
    @DisplayName("Should skip poll when lock is not acquired")
    void shouldSkipWhenLockNotAcquired() {
        when(lockPort.tryLock(anyString(), any(Duration.class)))
                .thenReturn(Optional.empty());

        scheduler.poll();

        verify(repository, never()).findOpenPayoutCandidates(100);
        verify(outboxRepository, never()).save(any());
    }
}

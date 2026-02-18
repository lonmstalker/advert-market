package com.advertmarket.deal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.deal.config.DealTimeoutProperties;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.outbox.OutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("DealTimeoutScheduler — expired deal processing")
@ExtendWith(MockitoExtension.class)
class DealTimeoutSchedulerTest {

    @Mock
    private DealRepository dealRepository;
    @Mock
    private DealTransitionService dealTransitionService;
    @Mock
    private DistributedLockPort lockPort;
    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private JsonFacade jsonFacade;
    @Mock
    private MetricsFacade metrics;

    private DealTimeoutProperties props;
    private DealTimeoutScheduler scheduler;

    @BeforeEach
    void setUp() {
        props = new DealTimeoutProperties(
                Duration.ofHours(48),
                Duration.ofHours(72),
                Duration.ofHours(24),
                Duration.ofHours(72),
                Duration.ofHours(72),
                Duration.ofHours(168),
                Duration.ofMinutes(5),
                50,
                Duration.ofMinutes(2));
        scheduler = new DealTimeoutScheduler(
                dealRepository, dealTransitionService,
                lockPort, outboxRepository, jsonFacade, metrics, props);
    }

    @Nested
    @DisplayName("processExpiredDeals")
    class ProcessExpiredDeals {

        @Test
        @DisplayName("Should skip when lock cannot be acquired")
        void skipsWhenLockUnavailable() {
            when(lockPort.tryLock(any(), any()))
                    .thenReturn(Optional.empty());

            scheduler.processExpiredDeals();

            verify(dealRepository, never()).findExpiredDeals(anyInt(), any());
        }

        @Test
        @DisplayName("Should process OFFER_PENDING deal as EXPIRED without refund")
        void expiresOfferPendingWithoutRefund() {
            acquireLock();
            var deal = dealRecord(DealStatus.OFFER_PENDING, false);
            when(dealRepository.findExpiredDeals(eq(50), any()))
                    .thenReturn(List.of(deal));
            when(dealRepository.findById(any())).thenReturn(Optional.of(deal));
            when(dealTransitionService.transition(any()))
                    .thenReturn(new DealTransitionResult.Success(DealStatus.EXPIRED));

            scheduler.processExpiredDeals();

            var captor = ArgumentCaptor.forClass(DealTransitionCommand.class);
            verify(dealTransitionService).transition(captor.capture());
            var cmd = captor.getValue();
            assertThat(cmd.targetStatus()).isEqualTo(DealStatus.EXPIRED);
            assertThat(cmd.actorType()).isEqualTo(ActorType.SYSTEM);
        }

        @Test
        @DisplayName("Should process FUNDED deal as EXPIRED with refund indication")
        void expiresFundedWithRefund() {
            acquireLock();
            var deal = dealRecord(DealStatus.FUNDED, true);
            when(dealRepository.findExpiredDeals(eq(50), any()))
                    .thenReturn(List.of(deal));
            when(dealRepository.findById(any())).thenReturn(Optional.of(deal));
            when(dealTransitionService.transition(any()))
                    .thenReturn(new DealTransitionResult.Success(DealStatus.EXPIRED));

            scheduler.processExpiredDeals();

            var captor = ArgumentCaptor.forClass(DealTransitionCommand.class);
            verify(dealTransitionService).transition(captor.capture());
            assertThat(captor.getValue().targetStatus())
                    .isEqualTo(DealStatus.EXPIRED);
        }

        @Test
        @DisplayName("Should handle deal already transitioned (idempotent)")
        void handlesAlreadyTransitioned() {
            acquireLock();
            var deal = dealRecord(DealStatus.OFFER_PENDING, false);
            when(dealRepository.findExpiredDeals(eq(50), any()))
                    .thenReturn(List.of(deal));
            when(dealRepository.findById(any())).thenReturn(Optional.of(deal));
            when(dealTransitionService.transition(any()))
                    .thenReturn(new DealTransitionResult.AlreadyInTargetState(
                            DealStatus.EXPIRED));

            scheduler.processExpiredDeals();

            verify(dealTransitionService).transition(any());
        }

        @Test
        @DisplayName("Should continue processing remaining deals when one fails")
        void continuesOnFailure() {
            acquireLock();
            var deal1 = dealRecord(DealStatus.OFFER_PENDING, false);
            var deal2 = dealRecord(DealStatus.NEGOTIATING, false);
            when(dealRepository.findExpiredDeals(eq(50), any()))
                    .thenReturn(List.of(deal1, deal2));
            when(dealRepository.findById(any()))
                    .thenReturn(Optional.of(deal1))
                    .thenReturn(Optional.of(deal2));
            when(dealTransitionService.transition(any()))
                    .thenThrow(new RuntimeException("CAS conflict"))
                    .thenReturn(new DealTransitionResult.Success(DealStatus.EXPIRED));

            scheduler.processExpiredDeals();

            verify(dealTransitionService, org.mockito.Mockito.times(2))
                    .transition(any());
        }

        @Test
        @DisplayName("Should process empty batch without errors")
        void handlesEmptyBatch() {
            acquireLock();
            when(dealRepository.findExpiredDeals(eq(50), any()))
                    .thenReturn(List.of());

            scheduler.processExpiredDeals();

            verify(dealTransitionService, never()).transition(any());
        }

        @Test
        @DisplayName("Should escalate DISPUTED timeout and clear deadline")
        void escalatesDisputedTimeoutAndClearsDeadline() {
            acquireLock();
            var deal = dealRecord(DealStatus.DISPUTED, false);
            when(dealRepository.findExpiredDeals(eq(50), any()))
                    .thenReturn(List.of(deal));
            when(dealRepository.findById(any())).thenReturn(Optional.of(deal));
            when(dealRepository.findOperatorUserIds())
                    .thenReturn(List.of(101L, 202L));
            when(jsonFacade.toJson(any())).thenReturn("{}");

            scheduler.processExpiredDeals();

            verify(dealTransitionService, never()).transition(any());
            verify(dealRepository).clearDeadline(any());
            verify(outboxRepository, org.mockito.Mockito.times(2))
                    .save(any());
        }

        @Test
        @DisplayName("Should release lock after processing")
        void releasesLock() {
            when(lockPort.tryLock(any(), any()))
                    .thenReturn(Optional.of("token-123"));
            when(dealRepository.findExpiredDeals(eq(50), any()))
                    .thenReturn(List.of());

            scheduler.processExpiredDeals();

            verify(lockPort).unlock(any(), eq("token-123"));
        }
    }

    private void acquireLock() {
        when(lockPort.tryLock(any(), any()))
                .thenReturn(Optional.of("test-token"));
    }

    private static DealRecord dealRecord(DealStatus status, boolean funded) {
        var id = UUID.randomUUID();
        return new DealRecord(
                id, -100L, 1L, 2L, null,
                status, 1_000_000_000L, 1000, 100_000_000L,
                funded ? "EQ..." : null,
                funded ? 42 : null,
                null, null, null, null,
                Instant.now().minusSeconds(3600),
                null, null,
                funded ? Instant.now().minusSeconds(7200) : null,
                null, null, null, null,
                0, Instant.now().minusSeconds(86400), Instant.now());
    }
}

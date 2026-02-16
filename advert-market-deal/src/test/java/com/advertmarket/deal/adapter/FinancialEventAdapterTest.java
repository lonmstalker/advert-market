package com.advertmarket.deal.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.deal.service.DealTransitionService;
import com.advertmarket.financial.api.event.DepositConfirmedEvent;
import com.advertmarket.financial.api.event.DepositFailedEvent;
import com.advertmarket.financial.api.event.DepositFailureReason;
import com.advertmarket.financial.api.event.PayoutCompletedEvent;
import com.advertmarket.financial.api.event.RefundCompletedEvent;
import com.advertmarket.financial.api.port.EscrowPort;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("FinancialEventAdapter — bridges financial events to deal transitions")
class FinancialEventAdapterTest {

    private DealTransitionService dealTransitionService;
    private EscrowPort escrowPort;
    private DealRepository dealRepository;
    private FinancialEventAdapter adapter;

    @BeforeEach
    void setUp() {
        dealTransitionService = mock(DealTransitionService.class);
        escrowPort = mock(EscrowPort.class);
        dealRepository = mock(DealRepository.class);
        adapter = new FinancialEventAdapter(
                dealTransitionService, escrowPort, dealRepository);
    }

    @Test
    @DisplayName("Should confirm deposit and transition deal to FUNDED on DepositConfirmed")
    void onDepositConfirmed() {
        var dealId = DealId.generate();
        var event = new DepositConfirmedEvent(
                "txhash1", 10_000_000_000L, 10_000_000_000L,
                3, "fromAddr", "UQaddr");
        var envelope = EventEnvelope.create(
                EventTypes.DEPOSIT_CONFIRMED, dealId, event);

        when(dealTransitionService.transition(any()))
                .thenReturn(new DealTransitionResult.Success(DealStatus.FUNDED));

        adapter.onDepositConfirmed(envelope);

        verify(dealRepository).setFunded(
                eq(dealId), any(), eq("txhash1"));
        verify(escrowPort).confirmDeposit(
                dealId, "txhash1", 10_000_000_000L, 10_000_000_000L,
                3, "fromAddr");

        var captor = ArgumentCaptor.forClass(DealTransitionCommand.class);
        verify(dealTransitionService).transition(captor.capture());
        var command = captor.getValue();
        assert command.targetStatus() == DealStatus.FUNDED;
    }

    @Test
    @DisplayName("Should transition deal to EXPIRED on DepositFailed with TIMEOUT")
    void onDepositFailed() {
        var dealId = DealId.generate();
        var event = new DepositFailedEvent(
                DepositFailureReason.TIMEOUT, 10_000_000_000L, 0L);
        var envelope = EventEnvelope.create(
                EventTypes.DEPOSIT_FAILED, dealId, event);

        when(dealTransitionService.transition(any()))
                .thenReturn(new DealTransitionResult.Success(DealStatus.EXPIRED));

        adapter.onDepositFailed(envelope);

        var captor = ArgumentCaptor.forClass(DealTransitionCommand.class);
        verify(dealTransitionService).transition(captor.capture());
        var command = captor.getValue();
        assert command.targetStatus() == DealStatus.EXPIRED;
    }

    @Test
    @DisplayName("Should transition deal to CANCELLED as advertiser on DepositFailed with REJECTED")
    void onDepositFailed_rejectedUsesAdvertiserActor() {
        var dealId = DealId.generate();
        long advertiserId = 42L;
        var event = new DepositFailedEvent(
                DepositFailureReason.REJECTED,
                10_000_000_000L,
                10_000_000_000L);
        var envelope = EventEnvelope.create(
                EventTypes.DEPOSIT_FAILED, dealId, event);

        when(dealRepository.findById(dealId))
                .thenReturn(Optional.of(new DealRecord(
                        dealId.value(),
                        1L,
                        advertiserId,
                        2L,
                        null,
                        DealStatus.AWAITING_PAYMENT,
                        10_000_000_000L,
                        1000,
                        1_000_000_000L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        Instant.now(),
                        Instant.now())));
        when(dealTransitionService.transition(any()))
                .thenReturn(new DealTransitionResult.Success(DealStatus.CANCELLED));

        adapter.onDepositFailed(envelope);

        var captor = ArgumentCaptor.forClass(DealTransitionCommand.class);
        verify(dealTransitionService).transition(captor.capture());
        var command = captor.getValue();
        assert command.targetStatus() == DealStatus.CANCELLED;
        assert command.actorType() == ActorType.ADVERTISER;
        assert command.actorId() == advertiserId;
    }

    @Test
    @DisplayName("Should persist payout tx hash on PayoutCompleted")
    void onPayoutCompleted() {
        var dealId = DealId.generate();
        var event = new PayoutCompletedEvent(
                "payout-tx-1", 900_000_000L, 100_000_000L,
                "UQ_owner", 1);
        var envelope = EventEnvelope.create(
                EventTypes.PAYOUT_COMPLETED, dealId, event);

        adapter.onPayoutCompleted(envelope);

        verify(dealRepository).setPayoutTxHash(dealId, "payout-tx-1");
    }

    @Test
    @DisplayName("Should persist refund tx hash on RefundCompleted")
    void onRefundCompleted() {
        var dealId = DealId.generate();
        var event = new RefundCompletedEvent(
                "refund-tx-1", 1_000_000_000L, "UQ_adv", 1);
        var envelope = EventEnvelope.create(
                EventTypes.REFUND_COMPLETED, dealId, event);

        adapter.onRefundCompleted(envelope);

        verify(dealRepository).setRefundedTxHash(dealId, "refund-tx-1");
    }
}

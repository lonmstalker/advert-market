package com.advertmarket.deal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.event.DealStateChangedEvent;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.financial.api.model.DepositAddressInfo;
import com.advertmarket.financial.api.port.DepositPort;
import com.advertmarket.financial.api.port.EscrowPort;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DealWorkflowEngine")
class DealWorkflowEngineTest {

    private DealRepository dealRepository;
    private DealTransitionService dealTransitionService;
    private EscrowPort escrowPort;
    private DepositPort depositPort;
    private OutboxRepository outboxRepository;
    private ChannelRepository channelRepository;
    private JsonFacade jsonFacade;
    private DealWorkflowEngine engine;

    @BeforeEach
    void setUp() {
        dealRepository = mock(DealRepository.class);
        dealTransitionService = mock(DealTransitionService.class);
        escrowPort = mock(EscrowPort.class);
        depositPort = mock(DepositPort.class);
        outboxRepository = mock(OutboxRepository.class);
        channelRepository = mock(ChannelRepository.class);
        jsonFacade = mock(JsonFacade.class);
        when(jsonFacade.toJson(any())).thenReturn("{}");
        engine = new DealWorkflowEngine(
                dealRepository,
                dealTransitionService,
                escrowPort,
                depositPort,
                outboxRepository,
                channelRepository,
                jsonFacade);
    }

    @Test
    @DisplayName("AWAITING_PAYMENT: generate deposit address, persist and emit WATCH_DEPOSIT")
    void awaitingPayment_generatesDepositAddressAndWatchCommand() {
        var dealId = DealId.generate();
        var deal = new DealRecord(
                dealId.value(),
                -100300L,
                11L,
                22L,
                null,
                DealStatus.AWAITING_PAYMENT,
                1_000_000_000L,
                1000,
                100_000_000L,
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
                Instant.now());
        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(escrowPort.generateDepositAddress(dealId, deal.amountNano()))
                .thenReturn(new DepositAddressInfo("UQ_deposit_1", 101));

        var event = new DealStateChangedEvent(
                DealStatus.ACCEPTED,
                DealStatus.AWAITING_PAYMENT,
                null,
                ActorType.SYSTEM,
                deal.amountNano(),
                deal.channelId(),
                null,
                null);
        var envelope = EventEnvelope.create(
                EventTypes.DEAL_STATE_CHANGED, dealId, event);

        engine.handle(envelope);

        verify(dealRepository).setDepositAddress(
                dealId, "UQ_deposit_1", 101);
        verify(dealRepository).setDeadline(eq(dealId), any());
        verify(outboxRepository).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("PUBLISHED: emit VERIFY_DELIVERY and auto-transition to DELIVERY_VERIFYING")
    void published_emitsVerifyDeliveryAndAutoTransition() {
        var dealId = DealId.generate();
        var deal = new DealRecord(
                dealId.value(),
                -100200L,
                10L,
                20L,
                null,
                DealStatus.PUBLISHED,
                1_500_000_000L,
                1000,
                150_000_000L,
                "UQ_deposit_2",
                102,
                null,
                null,
                777L,
                "content-hash-1",
                null,
                Instant.now(),
                null,
                Instant.now(),
                null,
                "tx-deposit-1",
                null,
                null,
                0,
                Instant.now(),
                Instant.now());
        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

        var event = new DealStateChangedEvent(
                DealStatus.SCHEDULED,
                DealStatus.PUBLISHED,
                null,
                ActorType.SYSTEM,
                deal.amountNano(),
                deal.channelId(),
                null,
                null);
        var envelope = EventEnvelope.create(
                EventTypes.DEAL_STATE_CHANGED, dealId, event);

        engine.handle(envelope);

        verify(outboxRepository).save(any(OutboxEntry.class));
        var commandCaptor = org.mockito.ArgumentCaptor.forClass(
                com.advertmarket.deal.api.dto.DealTransitionCommand.class);
        verify(dealTransitionService).transition(commandCaptor.capture());
        var command = commandCaptor.getValue();
        assertThat(command.dealId()).isEqualTo(dealId);
        assertThat(command.targetStatus()).isEqualTo(DealStatus.DELIVERY_VERIFYING);
        assertThat(command.actorType()).isEqualTo(ActorType.SYSTEM);
    }

    @Test
    @DisplayName("COMPLETED_RELEASED: release escrow and emit EXECUTE_PAYOUT")
    void completedReleased_releasesEscrowAndEmitsPayout() {
        var dealId = DealId.generate();
        var deal = deal(dealId, DealStatus.COMPLETED_RELEASED);
        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

        var event = new DealStateChangedEvent(
                DealStatus.DELIVERY_VERIFYING,
                DealStatus.COMPLETED_RELEASED,
                null,
                ActorType.SYSTEM,
                deal.amountNano(),
                deal.channelId(),
                null,
                null);
        var envelope = EventEnvelope.create(
                EventTypes.DEAL_STATE_CHANGED, dealId, event);

        engine.handle(envelope);

        verify(escrowPort).releaseEscrow(
                dealId,
                new UserId(deal.ownerId()),
                deal.amountNano(),
                deal.commissionRateBp());
        verify(outboxRepository, org.mockito.Mockito.atLeastOnce())
                .save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("PARTIALLY_REFUNDED: emit refund/payout commands from event amounts")
    void partiallyRefunded_emitsPartialCommands() {
        var dealId = DealId.generate();
        var deal = deal(dealId, DealStatus.PARTIALLY_REFUNDED);
        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(depositPort.findRefundAddress(dealId))
                .thenReturn(Optional.of("UQ_refund_to_advertiser"));

        var event = new DealStateChangedEvent(
                DealStatus.DISPUTED,
                DealStatus.PARTIALLY_REFUNDED,
                42L,
                ActorType.PLATFORM_OPERATOR,
                deal.amountNano(),
                deal.channelId(),
                700_000_000L,
                800_000_000L);
        var envelope = EventEnvelope.create(
                EventTypes.DEAL_STATE_CHANGED, dealId, event);

        engine.handle(envelope);

        verify(outboxRepository, org.mockito.Mockito.atLeast(2))
                .save(any(OutboxEntry.class));
    }

    private static DealRecord deal(DealId dealId, DealStatus status) {
        return new DealRecord(
                dealId.value(),
                -100100L,
                11L,
                22L,
                null,
                status,
                1_000_000_000L,
                1000,
                100_000_000L,
                "UQ_deposit",
                103,
                null,
                null,
                555L,
                "content-hash",
                null,
                Instant.now(),
                null,
                Instant.now(),
                null,
                "tx_deposit",
                null,
                null,
                0,
                Instant.now(),
                Instant.now());
    }

}

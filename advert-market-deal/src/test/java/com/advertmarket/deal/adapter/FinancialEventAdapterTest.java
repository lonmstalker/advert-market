package com.advertmarket.deal.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.service.DealTransitionService;
import com.advertmarket.financial.api.event.DepositConfirmedEvent;
import com.advertmarket.financial.api.event.DepositFailedEvent;
import com.advertmarket.financial.api.event.DepositFailureReason;
import com.advertmarket.financial.api.port.EscrowPort;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("FinancialEventAdapter — bridges financial events to deal transitions")
class FinancialEventAdapterTest {

    private DealTransitionService dealTransitionService;
    private EscrowPort escrowPort;
    private FinancialEventAdapter adapter;

    @BeforeEach
    void setUp() {
        dealTransitionService = mock(DealTransitionService.class);
        escrowPort = mock(EscrowPort.class);
        adapter = new FinancialEventAdapter(dealTransitionService, escrowPort);
    }

    @Test
    @DisplayName("Should confirm deposit and transition deal to FUNDED on DepositConfirmed")
    void onDepositConfirmed() {
        var dealId = DealId.generate();
        var event = new DepositConfirmedEvent(
                "txhash1", 10_000_000_000L, 3, "fromAddr", "UQaddr");
        var envelope = EventEnvelope.create(
                EventTypes.DEPOSIT_CONFIRMED, dealId, event);

        when(dealTransitionService.transition(any()))
                .thenReturn(new DealTransitionResult.Success(DealStatus.FUNDED));

        adapter.onDepositConfirmed(envelope);

        verify(escrowPort).confirmDeposit(
                dealId, "txhash1", 10_000_000_000L, 3, "fromAddr");

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
}

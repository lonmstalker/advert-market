package com.advertmarket.deal.adapter;

import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.service.DealTransitionService;
import com.advertmarket.financial.api.event.DepositConfirmedEvent;
import com.advertmarket.financial.api.event.DepositFailedEvent;
import com.advertmarket.financial.api.event.PayoutCompletedEvent;
import com.advertmarket.financial.api.event.RefundCompletedEvent;
import com.advertmarket.financial.api.port.EscrowPort;
import com.advertmarket.financial.api.port.FinancialEventPort;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealStatus;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Bridges financial result events to deal state transitions.
 *
 * <p>NOT {@code @Component} — wired via
 * {@link com.advertmarket.deal.config.DealConfig}.
 */
@Slf4j
@RequiredArgsConstructor
public class FinancialEventAdapter implements FinancialEventPort {

    private final DealTransitionService dealTransitionService;
    private final EscrowPort escrowPort;

    @Override
    public void onDepositConfirmed(
            @NonNull EventEnvelope<DepositConfirmedEvent> envelope) {
        var dealId = Objects.requireNonNull(envelope.dealId(),
                "dealId required for deposit confirmed event");
        var event = envelope.payload();

        escrowPort.confirmDeposit(dealId, event.txHash(),
                event.amountNano(), event.confirmations(),
                event.fromAddress());

        dealTransitionService.transition(new DealTransitionCommand(
                dealId, DealStatus.FUNDED, null,
                ActorType.SYSTEM, "Deposit confirmed: " + event.txHash()));

        log.info("Deposit confirmed for deal={}, txHash={}",
                dealId, event.txHash());
    }

    @Override
    public void onDepositFailed(
            @NonNull EventEnvelope<DepositFailedEvent> envelope) {
        var dealId = Objects.requireNonNull(envelope.dealId(),
                "dealId required for deposit failed event");
        var event = envelope.payload();

        dealTransitionService.transition(new DealTransitionCommand(
                dealId, DealStatus.EXPIRED, null,
                ActorType.SYSTEM,
                "Deposit failed: " + event.reason()));

        log.info("Deposit failed for deal={}, reason={}",
                dealId, event.reason());
    }

    @Override
    public void onPayoutCompleted(
            @NonNull EventEnvelope<PayoutCompletedEvent> envelope) {
        log.info("Payout completed for deal={}", envelope.dealId());
    }

    @Override
    public void onRefundCompleted(
            @NonNull EventEnvelope<RefundCompletedEvent> envelope) {
        log.info("Refund completed for deal={}", envelope.dealId());
    }
}

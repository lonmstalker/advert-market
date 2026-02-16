package com.advertmarket.deal.adapter;

import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.port.DealRepository;
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
import java.time.Instant;
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
    private final DealRepository dealRepository;

    @Override
    public void onDepositConfirmed(
            @NonNull EventEnvelope<DepositConfirmedEvent> envelope) {
        var dealId = Objects.requireNonNull(envelope.dealId(),
                "dealId required for deposit confirmed event");
        var event = envelope.payload();
        dealRepository.setFunded(dealId, Instant.now(), event.txHash());

        escrowPort.confirmDeposit(dealId, event.txHash(),
                event.amountNano(), event.expectedAmountNano(),
                event.confirmations(), event.fromAddress());

        dealTransitionService.transition(new DealTransitionCommand(
                dealId, DealStatus.FUNDED, null,
                ActorType.SYSTEM, "Deposit confirmed: " + event.txHash(),
                null, null));

        log.info("Deposit confirmed for deal={}, txHash={}",
                dealId, event.txHash());
    }

    @Override
    public void onDepositFailed(
            @NonNull EventEnvelope<DepositFailedEvent> envelope) {
        var dealId = Objects.requireNonNull(envelope.dealId(),
                "dealId required for deposit failed event");
        var event = envelope.payload();
        var targetStatus = switch (event.reason()) {
            case TIMEOUT -> DealStatus.EXPIRED;
            case REJECTED -> DealStatus.CANCELLED;
            case AMOUNT_MISMATCH -> DealStatus.CANCELLED;
        };
        var actor = resolveActorForDepositFailure(dealId, targetStatus);

        dealTransitionService.transition(new DealTransitionCommand(
                dealId,
                targetStatus,
                actor.actorId(),
                actor.actorType(),
                "Deposit failed: " + event.reason(),
                null,
                null));

        log.info("Deposit failed for deal={}, reason={}",
                dealId, event.reason());
    }

    private TransitionActor resolveActorForDepositFailure(
            com.advertmarket.shared.model.DealId dealId,
            DealStatus targetStatus) {
        if (targetStatus != DealStatus.CANCELLED) {
            return new TransitionActor(ActorType.SYSTEM, null);
        }
        var deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalStateException(
                        "Deal not found for deposit failure " + dealId));
        return new TransitionActor(ActorType.ADVERTISER, deal.advertiserId());
    }

    private record TransitionActor(ActorType actorType, Long actorId) {
    }

    @Override
    public void onPayoutCompleted(
            @NonNull EventEnvelope<PayoutCompletedEvent> envelope) {
        var dealId = Objects.requireNonNull(envelope.dealId(),
                "dealId required for payout completed event");
        dealRepository.setPayoutTxHash(dealId, envelope.payload().txHash());
        log.info("Payout completed for deal={}, txHash={}",
                dealId, envelope.payload().txHash());
    }

    @Override
    public void onRefundCompleted(
            @NonNull EventEnvelope<RefundCompletedEvent> envelope) {
        var dealId = Objects.requireNonNull(envelope.dealId(),
                "dealId required for refund completed event");
        dealRepository.setRefundedTxHash(dealId, envelope.payload().txHash());
        log.info("Refund completed for deal={}, txHash={}",
                dealId, envelope.payload().txHash());
    }
}

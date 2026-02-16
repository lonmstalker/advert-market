package com.advertmarket.financial.ton.service;

import com.advertmarket.financial.api.event.ExecuteRefundCommand;
import com.advertmarket.financial.api.event.RefundCompletedEvent;
import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.RefundExecutorPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.shared.event.DomainEvent;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import com.advertmarket.shared.outbox.OutboxStatus;
import com.advertmarket.shared.util.IdempotencyKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Executes TON refunds to advertisers after deal
 * cancellation or dispute resolution.
 *
 * <p>Supports both full and partial refunds.
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"fenum:argument", "fenum:assignment"})
public class RefundExecutorWorker implements RefundExecutorPort {

    private static final Duration LOCK_TTL = Duration.ofSeconds(60);

    private final TonWalletPort tonWalletPort;
    private final LedgerPort ledgerPort;
    private final OutboxRepository outboxRepository;
    private final DistributedLockPort lockPort;
    private final JsonFacade jsonFacade;
    private final MetricsFacade metrics;

    @Override
    public void executeRefund(
            @NonNull EventEnvelope<ExecuteRefundCommand> envelope) {
        var command = envelope.payload();
        var dealId = envelope.dealId();
        if (dealId == null) {
            log.error("EXECUTE_REFUND without dealId, eventId={}",
                    envelope.eventId());
            return;
        }

        String lockKey = "lock:refund:" + dealId.value();
        lockPort.withLock(lockKey, LOCK_TTL, () -> {
            doExecuteRefund(dealId, command);
            return null;
        });
    }

    private void doExecuteRefund(DealId dealId,
            ExecuteRefundCommand command) {
        String txHash = tonWalletPort.submitTransaction(
                command.subwalletId(),
                command.refundAddress(),
                command.amountNano());

        log.info("Refund TX submitted: deal={}, txHash={}, "
                        + "to={}, amount={}",
                dealId, txHash, command.refundAddress(),
                command.amountNano());

        recordLedger(dealId, command.amountNano());
        publishCompletedEvent(dealId, txHash, command);

        metrics.incrementCounter(MetricNames.REFUND_COMPLETED);
    }

    private void recordLedger(DealId dealId, long amountNano) {
        var amount = Money.ofNano(amountNano);
        var transfer = new TransferRequest(
                dealId,
                IdempotencyKey.refund(dealId),
                List.of(
                        new Leg(AccountId.escrow(dealId),
                                EntryType.ESCROW_REFUND, amount,
                                Leg.Side.DEBIT),
                        new Leg(AccountId.externalTon(),
                                EntryType.ESCROW_REFUND, amount,
                                Leg.Side.CREDIT)),
                "Refund for deal " + dealId);
        ledgerPort.transfer(transfer);
    }

    private void publishCompletedEvent(DealId dealId, String txHash,
            ExecuteRefundCommand command) {
        var event = new RefundCompletedEvent(
                txHash, command.amountNano(),
                command.refundAddress(), 1);
        publishOutboxEvent(dealId, EventTypes.REFUND_COMPLETED, event);
    }

    private <T extends DomainEvent> void publishOutboxEvent(
            DealId dealId, String eventType, T event) {
        var envelope = EventEnvelope.create(eventType, dealId, event);
        outboxRepository.save(OutboxEntry.builder()
                .dealId(dealId)
                .topic(TopicNames.FINANCIAL_EVENTS)
                .partitionKey(dealId.value().toString())
                .payload(jsonFacade.toJson(envelope))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .version(0)
                .createdAt(Instant.now())
                .build());
    }
}

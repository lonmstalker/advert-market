package com.advertmarket.financial.ton.service;

import com.advertmarket.db.generated.tables.records.TonTransactionsRecord;
import com.advertmarket.financial.api.event.ExecutePayoutCommand;
import com.advertmarket.financial.api.event.PayoutCompletedEvent;
import com.advertmarket.financial.api.event.PayoutDeferredEvent;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.PayoutExecutorPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.event.DomainEvent;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import com.advertmarket.shared.outbox.OutboxStatus;
import com.advertmarket.shared.util.IdempotencyKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Executes TON payouts to channel owners after deal completion.
 *
 * <p>Acquires a distributed lock per deal, submits the on-chain
 * transfer, records ledger entries, and publishes a completion event.
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"fenum:argument", "fenum:assignment"})
public class PayoutExecutorWorker implements PayoutExecutorPort {

    private static final Duration LOCK_TTL = Duration.ofSeconds(60);
    private static final String TX_TYPE = "PAYOUT";

    private final TonWalletPort tonWalletPort;
    private final LedgerPort ledgerPort;
    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;
    private final DistributedLockPort lockPort;
    private final JsonFacade jsonFacade;
    private final MetricsFacade metrics;
    private final JooqTonTransactionRepository txRepository;

    @Override
    public void executePayout(
            @NonNull EventEnvelope<ExecutePayoutCommand> envelope) {
        var command = envelope.payload();
        var dealId = envelope.dealId();
        if (dealId == null) {
            log.error("EXECUTE_PAYOUT without dealId, eventId={}",
                    envelope.eventId());
            return;
        }

        String lockKey = "lock:payout:" + dealId.value();
        lockPort.withLock(lockKey, LOCK_TTL, () -> {
            doExecutePayout(dealId, command);
            return null;
        });
    }

    private void doExecutePayout(DealId dealId,
            ExecutePayoutCommand command) {
        var ownerId = new UserId(command.ownerId());
        var tonAddress = userRepository.findTonAddress(ownerId);

        if (tonAddress.isEmpty()) {
            handleDeferred(dealId, command);
            return;
        }

        String payoutAddress = tonAddress.get();
        TxRef txRef = resolveOrSubmitTx(
                dealId,
                command,
                payoutAddress);

        metrics.incrementCounter(MetricNames.PAYOUT_SUBMITTED);
        log.info("Payout TX submitted: deal={}, txHash={}, "
                        + "to={}, amount={}",
                dealId, txRef.txHash(), payoutAddress,
                command.amountNano());

        recordLedger(dealId, ownerId, command.amountNano());
        publishCompletedEvent(dealId, txRef.txHash(), command, payoutAddress);
        markOutboundConfirmed(txRef);

        metrics.incrementCounter(MetricNames.PAYOUT_COMPLETED);
    }

    private TxRef resolveOrSubmitTx(
            DealId dealId,
            ExecutePayoutCommand command,
            String payoutAddress) {
        var existing = txRepository.findLatestOutboundByDealIdAndType(
                dealId.value(), TX_TYPE);
        if (existing.isPresent()) {
            return reuseOrFail(dealId, existing.get());
        }

        long txId = txRepository.createOutbound(
                dealId.value(),
                TX_TYPE,
                command.amountNano(),
                payoutAddress,
                command.subwalletId());
        int version = 0;
        try {
            String txHash = tonWalletPort.submitTransaction(
                    command.subwalletId(),
                    payoutAddress,
                    command.amountNano());
            boolean marked = txRepository.markSubmitted(
                    txId, txHash, version);
            if (!marked) {
                throw new DomainException(
                        ErrorCodes.TON_TX_FAILED,
                        "Failed to persist outbound payout submission");
            }
            return new TxRef(txId, txHash, version + 1, false);
        } catch (RuntimeException ex) {
            txRepository.updateStatus(txId, "ABANDONED", 0, version);
            throw ex;
        }
    }

    private TxRef reuseOrFail(
            DealId dealId,
            TonTransactionsRecord record) {
        String status = record.getStatus() == null
                ? "" : record.getStatus().toUpperCase(Locale.ROOT);
        String txHash = record.getTxHash();
        int version = record.getVersion() == null
                ? 0 : record.getVersion();

        if (("SUBMITTED".equals(status) || "CONFIRMED".equals(status))
                && txHash != null && !txHash.isBlank()) {
            return new TxRef(
                    record.getId(),
                    txHash,
                    version,
                    "CONFIRMED".equals(status));
        }

        throw new DomainException(
                ErrorCodes.TON_TX_FAILED,
                "Outbound payout requires reconciliation before retry: deal="
                        + dealId.value() + ", txId=" + record.getId()
                        + ", status=" + status);
    }

    private void markOutboundConfirmed(TxRef txRef) {
        if (txRef.alreadyConfirmed()) {
            return;
        }
        txRepository.updateStatus(
                txRef.id(), "CONFIRMED", 0, txRef.version());
    }

    private void recordLedger(DealId dealId, UserId ownerId,
            long amountNano) {
        var amount = Money.ofNano(amountNano);
        var transfer = new TransferRequest(
                dealId,
                IdempotencyKey.payout(dealId),
                List.of(
                        new Leg(AccountId.ownerPending(ownerId),
                                EntryType.OWNER_WITHDRAWAL, amount,
                                Leg.Side.DEBIT),
                        new Leg(AccountId.externalTon(),
                                EntryType.OWNER_WITHDRAWAL, amount,
                                Leg.Side.CREDIT)),
                "Payout for deal " + dealId);
        ledgerPort.transfer(transfer);
    }

    private void publishCompletedEvent(DealId dealId, String txHash,
            ExecutePayoutCommand command, String toAddress) {
        var event = new PayoutCompletedEvent(
                txHash, command.amountNano(),
                command.commissionNano(), toAddress, 1);
        publishOutboxEvent(dealId, EventTypes.PAYOUT_COMPLETED, event);
    }

    private void handleDeferred(DealId dealId,
            ExecutePayoutCommand command) {
        log.warn("Payout deferred: owner={} has no TON address, "
                        + "deal={}", command.ownerId(), dealId);
        var event = new PayoutDeferredEvent(
                command.ownerId(), command.amountNano());
        publishOutboxEvent(dealId, EventTypes.PAYOUT_DEFERRED, event);
        metrics.incrementCounter(MetricNames.PAYOUT_DEFERRED);
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

    private record TxRef(
            long id,
            String txHash,
            int version,
            boolean alreadyConfirmed) {
    }
}

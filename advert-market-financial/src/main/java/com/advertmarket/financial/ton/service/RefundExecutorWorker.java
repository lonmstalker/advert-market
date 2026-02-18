package com.advertmarket.financial.ton.service;

import com.advertmarket.db.generated.tables.records.TonTransactionsRecord;
import com.advertmarket.financial.api.event.ExecuteRefundCommand;
import com.advertmarket.financial.api.event.RefundCompletedEvent;
import com.advertmarket.financial.api.event.RefundDeferredEvent;
import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.RefundExecutorPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
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
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
    private static final String TX_TYPE = "REFUND";

    private final TonWalletPort tonWalletPort;
    private final LedgerPort ledgerPort;
    private final OutboxRepository outboxRepository;
    private final DistributedLockPort lockPort;
    private final JsonFacade jsonFacade;
    private final MetricsFacade metrics;
    private final JooqTonTransactionRepository txRepository;

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
        TxRef txRef;
        try {
            txRef = resolveOrSubmitTx(dealId, command);
        } catch (DomainException exception) {
            if (shouldDefer(exception)) {
                handleDeferred(dealId, command, exception);
                return;
            }
            throw exception;
        }

        log.info("Refund TX submitted: deal={}, txHash={}, "
                        + "to={}, amount={}",
                dealId, txRef.txHash(), command.refundAddress(),
                command.amountNano());

        recordLedger(dealId, command.amountNano());
        publishCompletedEvent(dealId, txRef.txHash(), command);
        markOutboundConfirmed(txRef);

        metrics.incrementCounter(MetricNames.REFUND_COMPLETED);
    }

    private TxRef resolveOrSubmitTx(
            DealId dealId,
            ExecuteRefundCommand command) {
        var existing = txRepository.findLatestOutboundByDealIdAndType(
                dealId.value(), TX_TYPE);
        if (existing.isPresent()) {
            return reuseOrResumeCreatedOrFail(
                    dealId,
                    command,
                    existing.get());
        }

        long txId = txRepository.createOutbound(
                dealId.value(),
                TX_TYPE,
                command.amountNano(),
                command.refundAddress(),
                command.subwalletId());
        return submitAndMark(
                txId,
                0,
                command.subwalletId(),
                command.refundAddress(),
                command.amountNano(),
                "Failed to persist outbound refund submission");
    }

    private TxRef reuseOrResumeCreatedOrFail(
            DealId dealId,
            ExecuteRefundCommand command,
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

        if ("CREATED".equals(status)
                && (txHash == null || txHash.isBlank())) {
            return submitAndMark(
                    record.getId(),
                    version,
                    command.subwalletId(),
                    command.refundAddress(),
                    command.amountNano(),
                    "Failed to persist resumed outbound refund submission");
        }

        throw new DomainException(
                ErrorCodes.TON_TX_FAILED,
                "Outbound refund requires reconciliation before retry: deal="
                        + dealId.value() + ", txId=" + record.getId()
                        + ", status=" + status);
    }

    private TxRef submitAndMark(
            long txId,
            int expectedVersion,
            int subwalletId,
            String refundAddress,
            long amountNano,
            String persistFailureMessage) {
        // CHECKSTYLE.OFF: IllegalCatch
        try {
            String txHash = tonWalletPort.submitTransaction(
                    subwalletId,
                    refundAddress,
                    amountNano);
            boolean marked = txRepository.markSubmitted(
                    txId,
                    txHash,
                    expectedVersion);
            if (!marked) {
                throw new DomainException(
                        ErrorCodes.TON_TX_FAILED,
                        persistFailureMessage);
            }
            return new TxRef(
                    txId,
                    txHash,
                    expectedVersion + 1,
                    false);
        } catch (RuntimeException ex) {
            var domainException = extractDomainException(ex);
            if (isRetryableSubmissionFailure(domainException)) {
                txRepository.incrementRetryCount(txId);
                if (domainException != null) {
                    throw domainException;
                }
                throw ex;
            }
            txRepository.updateStatus(txId, "ABANDONED", 0, expectedVersion);
            if (domainException != null) {
                throw domainException;
            }
            throw ex;
        }
        // CHECKSTYLE.ON: IllegalCatch
    }

    private void markOutboundConfirmed(TxRef txRef) {
        if (txRef.alreadyConfirmed()) {
            return;
        }
        txRepository.updateStatus(
                txRef.id(), "CONFIRMED", 0, txRef.version());
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

    private void handleDeferred(
            DealId dealId,
            ExecuteRefundCommand command,
            DomainException exception) {
        log.warn(
                "Refund deferred due non-retryable TON error:"
                        + " deal={}, advertiserId={}, code={}, message={}",
                dealId,
                command.advertiserId(),
                exception.getErrorCode(),
                exception.getMessage());
        var event = new RefundDeferredEvent(
                command.advertiserId(),
                command.amountNano());
        publishOutboxEvent(dealId, EventTypes.REFUND_DEFERRED, event);
        metrics.incrementCounter(MetricNames.REFUND_DEFERRED);
    }

    @SuppressWarnings("ReferenceEquality")
    private static boolean shouldDefer(DomainException exception) {
        if (exception.getErrorCode() == ErrorCodes.TON_TX_FAILED) {
            var message = exception.getMessage();
            return message != null
                    && message.contains("requires reconciliation before retry");
        }
        if (exception.getErrorCode() == ErrorCodes.TON_API_ERROR) {
            var message = exception.getMessage();
            return message != null
                    && message.contains("getSeqno")
                    && message.contains("exitCode: -13");
        }
        return false;
    }

    @SuppressWarnings("ReferenceEquality")
    private static boolean isRetryableSubmissionFailure(
            @Nullable DomainException exception) {
        if (exception == null) {
            return false;
        }
        if (exception.getErrorCode() == ErrorCodes.TON_API_ERROR) {
            return true;
        }
        if (exception.getErrorCode() == ErrorCodes.TON_TX_FAILED) {
            var message = exception.getMessage();
            if (message == null) {
                return false;
            }
            return message.contains("after 3 retries")
                    || message.contains("seqno is still unavailable")
                    || message.contains("Interrupted while waiting for TON wallet deployment");
        }
        return false;
    }

    private static @Nullable DomainException extractDomainException(
            Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DomainException domainException) {
                return domainException;
            }
            current = current.getCause();
        }
        return null;
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

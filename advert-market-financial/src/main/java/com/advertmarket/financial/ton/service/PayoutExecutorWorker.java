package com.advertmarket.financial.ton.service;

import com.advertmarket.db.generated.tables.records.TonTransactionsRecord;
import com.advertmarket.financial.api.event.ExecutePayoutCommand;
import com.advertmarket.financial.api.event.PayoutCompletedEvent;
import com.advertmarket.financial.api.event.PayoutDeferredEvent;
import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.PayoutExecutorPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.config.NetworkFeeProperties;
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
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import com.advertmarket.shared.outbox.OutboxStatus;
import com.advertmarket.shared.util.IdempotencyKey;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
    private final NetworkFeeProperties networkFeeProperties;

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
            handleDeferred(dealId, command, "owner has no TON address");
            return;
        }

        String payoutAddress = tonAddress.get();
        TxRef txRef;
        try {
            txRef = resolveOrSubmitTx(
                    dealId,
                    command,
                    payoutAddress);
        } catch (DomainException exception) {
            if (shouldDefer(exception)) {
                log.warn(
                        "Payout deferred due non-retryable TON error:"
                                + " deal={}, ownerId={}, code={}, message={}",
                        dealId,
                        command.ownerId(),
                        exception.getErrorCode(),
                        exception.getMessage());
                handleDeferred(
                        dealId,
                        command,
                        "non-retryable TON error: " + exception.getMessage());
                return;
            }
            throw exception;
        }

        metrics.incrementCounter(MetricNames.PAYOUT_SUBMITTED);
        log.info("Payout TX submitted: deal={}, txHash={}, "
                        + "to={}, amount={}",
                dealId, txRef.txHash(), payoutAddress,
                command.amountNano());

        recordLedger(
                dealId,
                ownerId,
                command.amountNano(),
                networkFeeProperties.defaultEstimateNano());
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
            return reuseOrResumeCreatedOrFail(
                    dealId,
                    command,
                    payoutAddress,
                    existing.get());
        }

        long txId = txRepository.createOutbound(
                dealId.value(),
                TX_TYPE,
                command.amountNano(),
                payoutAddress,
                command.subwalletId());
        return submitAndMark(
                txId,
                0,
                command.subwalletId(),
                payoutAddress,
                command.amountNano(),
                "Failed to persist outbound payout submission");
    }

    private TxRef reuseOrResumeCreatedOrFail(
            DealId dealId,
            ExecutePayoutCommand command,
            String payoutAddress,
            TonTransactionsRecord record) {
        String status = normalizeStatus(record);
        String txHash = record.getTxHash();
        int version = normalizeVersion(record);

        if (isReusableSubmittedOrConfirmed(status, txHash)) {
            return buildReusableTxRef(record, txHash, version, status);
        }
        if (isResumableCreated(status, txHash)) {
            return resumeCreatedOutbound(command, payoutAddress, record, version);
        }
        if (isResumableAbandoned(status, txHash)) {
            log.warn(
                    "Resuming ABANDONED outbound payout without tx hash:"
                            + " deal={}, txId={}",
                    dealId.value(),
                    record.getId());
            return resumeAbandonedOutbound(command, payoutAddress, record, version);
        }

        throw new DomainException(
                ErrorCodes.TON_TX_FAILED,
                "Outbound payout requires reconciliation before retry: deal="
                        + dealId.value() + ", txId=" + record.getId()
                        + ", status=" + status);
    }

    private static String normalizeStatus(TonTransactionsRecord record) {
        return record.getStatus() == null
                ? "" : record.getStatus().toUpperCase(Locale.ROOT);
    }

    private static int normalizeVersion(TonTransactionsRecord record) {
        return record.getVersion() == null ? 0 : record.getVersion();
    }

    private static boolean isReusableSubmittedOrConfirmed(
            String status, String txHash) {
        return ("SUBMITTED".equals(status) || "CONFIRMED".equals(status))
                && txHash != null
                && !txHash.isBlank();
    }

    private static boolean isResumableCreated(String status, String txHash) {
        return "CREATED".equals(status) && isBlank(txHash);
    }

    private static boolean isResumableAbandoned(String status, String txHash) {
        return "ABANDONED".equals(status) && isBlank(txHash);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static TxRef buildReusableTxRef(
            TonTransactionsRecord record,
            String txHash,
            int version,
            String status) {
        return new TxRef(
                record.getId(),
                txHash,
                version,
                "CONFIRMED".equals(status));
    }

    private TxRef resumeCreatedOutbound(
            ExecutePayoutCommand command,
            String payoutAddress,
            TonTransactionsRecord record,
            int version) {
        return submitAndMark(
                record.getId(),
                version,
                command.subwalletId(),
                payoutAddress,
                command.amountNano(),
                "Failed to persist resumed outbound payout submission");
    }

    private TxRef resumeAbandonedOutbound(
            ExecutePayoutCommand command,
            String payoutAddress,
            TonTransactionsRecord record,
            int version) {
        return submitAndMark(
                record.getId(),
                version,
                command.subwalletId(),
                payoutAddress,
                command.amountNano(),
                "Failed to persist resumed abandoned outbound payout submission");
    }

    private TxRef submitAndMark(
            long txId,
            int expectedVersion,
            int subwalletId,
            String toAddress,
            long amountNano,
            String persistFailureMessage) {
        // CHECKSTYLE.OFF: IllegalCatch
        try {
            String txHash = tonWalletPort.submitTransaction(
                    subwalletId,
                    toAddress,
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
            if (txHash.isBlank()) {
                throw new DomainException(
                        ErrorCodes.TON_TX_FAILED,
                        "Outbound payout tx hash is unresolved;"
                                + " requires reconciliation before retry");
            }
            return new TxRef(
                    txId,
                    txHash,
                    expectedVersion + 1,
                    false);
        } catch (RuntimeException ex) {
            var domainException = extractDomainException(ex);
            if (isRetryableSubmissionFailure(domainException)
                    || isRetryableTransientFailure(ex)) {
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

    private void recordLedger(
            DealId dealId,
            UserId ownerId,
            long amountNano,
            long feeEstimateNano) {
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

        if (feeEstimateNano <= 0) {
            return;
        }

        var feeAmount = Money.ofNano(feeEstimateNano);
        var feeTransfer = new TransferRequest(
                dealId,
                IdempotencyKey.fee("payout:" + dealId.value()),
                List.of(
                        new Leg(AccountId.platformTreasury(),
                                EntryType.NETWORK_FEE,
                                feeAmount,
                                Leg.Side.DEBIT),
                        new Leg(AccountId.networkFees(),
                                EntryType.NETWORK_FEE,
                                feeAmount,
                                Leg.Side.CREDIT)),
                "Payout network fee for deal " + dealId);
        ledgerPort.transfer(feeTransfer);
    }

    private void publishCompletedEvent(DealId dealId, String txHash,
            ExecutePayoutCommand command, String toAddress) {
        var event = new PayoutCompletedEvent(
                txHash, command.amountNano(),
                command.commissionNano(), toAddress, 1);
        publishOutboxEvent(dealId, EventTypes.PAYOUT_COMPLETED, event);
    }

    private void handleDeferred(DealId dealId,
            ExecutePayoutCommand command,
            String reason) {
        log.warn("Payout deferred: deal={}, owner={}, reason={}",
                dealId, command.ownerId(), reason);
        var event = new PayoutDeferredEvent(
                command.ownerId(), command.amountNano());
        publishOutboxEvent(dealId, EventTypes.PAYOUT_DEFERRED, event);
        metrics.incrementCounter(MetricNames.PAYOUT_DEFERRED);
    }

    @SuppressWarnings("ReferenceEquality")
    private static boolean shouldDefer(DomainException exception) {
        if (exception.getErrorCode() == ErrorCodes.TON_TX_FAILED) {
            var message = exception.getMessage();
            if (message != null
                    && message.contains("requires reconciliation before retry")) {
                return true;
            }
            return hasUninitializedWalletSignal(exception);
        }
        if (exception.getErrorCode() == ErrorCodes.TON_API_ERROR) {
            return hasUninitializedWalletSignal(exception);
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
            return message == null
                    || !message.contains("requires reconciliation before retry");
        }
        return false;
    }

    private static boolean isRetryableTransientFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CallNotPermittedException
                    || current instanceof BulkheadFullException
                    || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
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

    private static boolean hasUninitializedWalletSignal(Throwable throwable) {
        return containsInErrorChain(throwable, "exitCode: -13")
                || containsInErrorChain(
                        throwable,
                        "Failed to unpack account state");
    }

    private static boolean containsInErrorChain(
            Throwable throwable, String needle) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(needle)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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

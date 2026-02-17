package com.advertmarket.financial.ton.service;

import com.advertmarket.db.generated.tables.records.TonTransactionsRecord;
import com.advertmarket.financial.api.event.DepositConfirmedEvent;
import com.advertmarket.financial.api.event.DepositFailedEvent;
import com.advertmarket.financial.api.event.DepositFailureReason;
import com.advertmarket.financial.api.event.WatchDepositCommand;
import com.advertmarket.financial.api.model.TonTransactionInfo;
import com.advertmarket.financial.api.port.TonBlockchainPort;
import com.advertmarket.financial.config.TonProperties;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.shared.event.DomainEvent;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import com.advertmarket.shared.outbox.OutboxStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls for pending TON deposits and confirms them once enough
 * blockchain confirmations are observed.
 *
 * <p>Uses distributed locking to prevent concurrent processing
 * across multiple instances.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"fenum:argument", "fenum:assignment"})
public class DepositWatcher {

    private static final String LOCK_KEY = "scheduler:deposit-watcher";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final int TX_FETCH_LIMIT = 10;

    private final TonBlockchainPort blockchainPort;
    private final JooqTonTransactionRepository txRepository;
    private final ConfirmationPolicyService confirmationPolicy;
    private final OutboxRepository outboxRepository;
    private final DistributedLockPort lockPort;
    private final JsonFacade jsonFacade;
    private final MetricsFacade metrics;
    private final TonProperties.Deposit depositProps;

    /**
     * Scheduled poll for pending deposits.
     * Acquires a distributed lock, then processes each pending TX.
     */
    @Scheduled(fixedDelayString = "${app.ton.deposit.poll-interval:10000}")
    public void pollDeposits() {
        var token = lockPort.tryLock(LOCK_KEY, LOCK_TTL);
        if (token.isEmpty()) {
            log.debug("Could not acquire deposit watcher lock, skipping");
            return;
        }

        try {
            doPollDeposits();
        } finally {
            lockPort.unlock(LOCK_KEY, token.get());
        }
    }

    /**
     * Registers (or reuses) a deposit watch initiated from workflow commands.
     */
    public void watchDeposit(EventEnvelope<WatchDepositCommand> envelope) {
        var dealId = Objects.requireNonNull(
                envelope.dealId(), "dealId is required for WATCH_DEPOSIT");
        final var command = envelope.payload();

        var existing = txRepository.findLatestInboundByDealId(dealId.value());
        if (existing.isPresent()) {
            log.debug("Deposit watch already registered for deal={}", dealId);
            return;
        }

        var record = new TonTransactionsRecord();
        record.setDealId(dealId.value());
        record.setDirection("IN");
        record.setAmountNano(command.expectedAmountNano());
        record.setToAddress(command.depositAddress());
        record.setStatus("PENDING");
        record.setConfirmations(0);
        record.setVersion(0);
        record.setRetryCount(0);
        txRepository.save(record);
        log.info("Registered deposit watch: deal={}, address={}",
                dealId, command.depositAddress());
    }

    private void doPollDeposits() {
        var pending = txRepository.findPendingDeposits(
                depositProps.batchSize());
        if (pending.isEmpty()) {
            return;
        }

        log.info("Processing {} pending deposits", pending.size());
        long masterSeqno = blockchainPort.getMasterchainSeqno();

        for (var record : pending) {
            processOneSafely(record, masterSeqno);
        }
    }

    // CHECKSTYLE.OFF: IllegalCatch
    private void processOneSafely(TonTransactionsRecord record,
                                   long masterSeqno) {
        try {
            processOne(record, masterSeqno);
        } catch (RuntimeException ex) {
            log.warn("Failed to process deposit id={}: {}",
                    record.getId(), ex.getMessage());
            handleRetry(record);
        }
    }
    // CHECKSTYLE.ON: IllegalCatch

    private void handleRetry(TonTransactionsRecord record) {
        int newRetryCount = txRepository.incrementRetryCount(record.getId());
        if (newRetryCount > depositProps.maxRetries()) {
            txRepository.updateStatus(record.getId(), "FAILED",
                    0, record.getVersion());
            metrics.incrementCounter(MetricNames.TON_DEPOSIT_FAILED);
            log.error("Deposit id={} permanently failed after {} retries",
                    record.getId(), newRetryCount);
        }
    }

    private void processOne(TonTransactionsRecord record,
                            long masterSeqno) {
        if (isTimedOut(record)) {
            handleTimeout(record);
            return;
        }

        var observation = observeInboundDeposit(record);
        if (observation.isEmpty()) {
            return;
        }

        processObservedDeposit(record, masterSeqno, observation.get());
    }

    private Optional<DepositObservation> observeInboundDeposit(TonTransactionsRecord record) {
        String toAddress = record.getToAddress();
        Long expectedAmount = record.getAmountNano();
        if (toAddress == null || expectedAmount == null) {
            log.warn("Deposit id={} has null toAddress or amountNano",
                    record.getId());
            return Optional.empty();
        }

        var inboundTxs = blockchainPort.getTransactions(
                toAddress, TX_FETCH_LIMIT).stream()
                .filter(tx -> tx.amountNano() > 0)
                .toList();
        if (inboundTxs.isEmpty()) {
            return Optional.empty();
        }

        long totalReceived = inboundTxs.stream()
                .mapToLong(TonTransactionInfo::amountNano)
                .sum();
        final var latest = inboundTxs.stream()
                .max(Comparator.comparingLong(TonTransactionInfo::lt))
                .orElseThrow();
        return Optional.of(new DepositObservation(expectedAmount, totalReceived, latest));
    }

    private void processObservedDeposit(
            TonTransactionsRecord record,
            long masterSeqno,
            DepositObservation observation) {
        long expectedAmount = observation.expectedAmount();
        long totalReceived = observation.totalReceived();

        if (totalReceived < expectedAmount) {
            updateProgressStatus(record, "UNDERPAID", confirmations(record));
            return;
        }

        var requirement = confirmationPolicy.requiredConfirmations(
                totalReceived);
        int confirmedBlocks = resolveConfirmedBlocks(record, masterSeqno);
        if (confirmedBlocks < 0) {
            return;
        }

        if (confirmedBlocks < requirement.confirmations()) {
            updatePendingConfirmationStatus(
                    record,
                    totalReceived,
                    expectedAmount,
                    confirmedBlocks,
                    requirement.confirmations());
            return;
        }

        if (totalReceived > expectedAmount || requirement.operatorReview()) {
            updateProgressStatus(
                    record, "AWAITING_OPERATOR_REVIEW", confirmedBlocks);
            return;
        }

        confirmDeposit(record, observation.latestTx(), confirmedBlocks, totalReceived);
    }

    private void updatePendingConfirmationStatus(
            TonTransactionsRecord record,
            long totalReceived,
            long expectedAmount,
            int confirmedBlocks,
            int requiredConfirmations) {
        String status = record.getSeqno() == null
                ? "TX_DETECTED"
                : (totalReceived > expectedAmount ? "OVERPAID" : "CONFIRMING");
        updateProgressStatus(record, status, confirmedBlocks);
        log.debug("Deposit id={} has {}/{} confirmations, status={}",
                record.getId(), confirmedBlocks, requiredConfirmations, status);
    }

    private int resolveConfirmedBlocks(TonTransactionsRecord record,
                                       long masterSeqno) {
        if (record.getSeqno() != null) {
            return (int) (masterSeqno - record.getSeqno());
        }
        txRepository.updateSeqno(record.getId(), masterSeqno,
                version(record));
        return 0;
    }

    private void confirmDeposit(TonTransactionsRecord record,
                                TonTransactionInfo tx,
                                int confirmedBlocks,
                                long receivedAmountNano) {
        boolean updated = txRepository.updateConfirmed(
                record.getId(), tx.txHash(), confirmedBlocks,
                tx.feeNano(),
                OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(tx.utime()),
                        ZoneOffset.UTC),
                Objects.requireNonNullElse(tx.fromAddress(), ""),
                version(record));

        if (!updated) {
            log.warn("Deposit id={} already confirmed by another instance",
                    record.getId());
            return;
        }

        String fromAddr = Objects.requireNonNullElse(
                tx.fromAddress(), "");
        String toAddr = Objects.requireNonNull(
                record.getToAddress(), "toAddress");
        var event = new DepositConfirmedEvent(
                tx.txHash(), receivedAmountNano,
                requiredAmount(record), confirmedBlocks,
                fromAddr, toAddr);

        publishOutboxEvent(record,
                EventTypes.DEPOSIT_CONFIRMED, event);

        metrics.incrementCounter(MetricNames.TON_DEPOSIT_CONFIRMED);
        log.info("Deposit confirmed: id={}, txHash={}, "
                        + "amount={}, confirmations={}",
                record.getId(), tx.txHash(),
                receivedAmountNano, confirmedBlocks);
    }

    private boolean isTimedOut(TonTransactionsRecord record) {
        if (record.getCreatedAt() == null) {
            return false;
        }
        var elapsed = Duration.between(
                record.getCreatedAt().toInstant(), Instant.now());
        return elapsed.compareTo(depositProps.maxPollDuration()) > 0;
    }

    private void handleTimeout(TonTransactionsRecord record) {
        boolean updated = txRepository.updateStatus(
                record.getId(), "TIMEOUT", 0, version(record));
        if (!updated) {
            return;
        }

        var event = new DepositFailedEvent(
                DepositFailureReason.TIMEOUT,
                requiredAmount(record), 0L);

        publishOutboxEvent(record,
                EventTypes.DEPOSIT_FAILED, event);

        metrics.incrementCounter(MetricNames.TON_DEPOSIT_TIMEOUT);
        log.info("Deposit timed out: id={}, dealId={}",
                record.getId(), record.getDealId());
    }

    private void updateProgressStatus(
            TonTransactionsRecord record,
            String status,
            int confirmations) {
        int currentConfirmations = confirmations(record);
        if (status.equals(record.getStatus())
                && confirmations == currentConfirmations) {
            return;
        }
        boolean updated = txRepository.updateStatus(
                record.getId(),
                status,
                confirmations,
                version(record));
        if (!updated) {
            log.debug("Deposit progress CAS skipped: id={}, targetStatus={}",
                    record.getId(), status);
        }
    }

    private static int confirmations(TonTransactionsRecord record) {
        return record.getConfirmations() != null
                ? record.getConfirmations()
                : 0;
    }

    private static int version(TonTransactionsRecord record) {
        return record.getVersion() != null
                ? record.getVersion()
                : 0;
    }

    private static long requiredAmount(TonTransactionsRecord record) {
        return Objects.requireNonNull(
                record.getAmountNano(),
                "amount_nano must be present for deposit");
    }

    private record DepositObservation(
            long expectedAmount,
            long totalReceived,
            TonTransactionInfo latestTx
    ) {
    }

    private <T extends DomainEvent> void publishOutboxEvent(
            TonTransactionsRecord record,
            String eventType, T event) {
        DealId dealId = record.getDealId() != null
                ? DealId.of(record.getDealId()) : null;
        var envelope = EventEnvelope.create(eventType, dealId, event);

        outboxRepository.save(OutboxEntry.builder()
                .dealId(dealId)
                .topic(TopicNames.FINANCIAL_EVENTS)
                .partitionKey(record.getDealId() != null
                        ? record.getDealId().toString() : null)
                .payload(jsonFacade.toJson(envelope))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .version(0)
                .createdAt(Instant.now())
                .build());
    }
}

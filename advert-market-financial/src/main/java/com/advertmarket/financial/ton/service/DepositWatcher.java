package com.advertmarket.financial.ton.service;

import com.advertmarket.db.generated.tables.records.TonTransactionsRecord;
import com.advertmarket.financial.api.event.DepositConfirmedEvent;
import com.advertmarket.financial.api.event.DepositFailedEvent;
import com.advertmarket.financial.api.event.DepositFailureReason;
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
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
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
        }
    }
    // CHECKSTYLE.ON: IllegalCatch

    private void processOne(TonTransactionsRecord record,
                             long masterSeqno) {
        if (isTimedOut(record)) {
            handleTimeout(record);
            return;
        }

        String toAddress = record.getToAddress();
        Long expectedAmount = record.getAmountNano();
        if (toAddress == null || expectedAmount == null) {
            log.warn("Deposit id={} has null toAddress or amountNano",
                    record.getId());
            return;
        }

        var match = findMatchingTx(toAddress, expectedAmount);
        if (match == null) {
            return;
        }

        var requirement = confirmationPolicy.requiredConfirmations(
                match.amountNano());
        int confirmedBlocks = resolveConfirmedBlocks(record, masterSeqno);
        if (confirmedBlocks < 0) {
            return;
        }

        if (confirmedBlocks < requirement.confirmations()) {
            log.debug("Deposit id={} has {}/{} confirmations",
                    record.getId(), confirmedBlocks,
                    requirement.confirmations());
            return;
        }

        confirmDeposit(record, match, confirmedBlocks);
    }

    private @Nullable TonTransactionInfo findMatchingTx(
            String toAddress, long expectedAmount) {
        List<TonTransactionInfo> txs = blockchainPort.getTransactions(
                toAddress, TX_FETCH_LIMIT);

        return txs.stream()
                .filter(tx -> tx.amountNano() >= expectedAmount)
                .findFirst()
                .orElse(null);
    }

    private int resolveConfirmedBlocks(TonTransactionsRecord record,
                                        long masterSeqno) {
        if (record.getSeqno() != null) {
            return (int) (masterSeqno - record.getSeqno());
        }
        txRepository.updateSeqno(record.getId(), masterSeqno,
                record.getVersion());
        return 0;
    }

    private void confirmDeposit(TonTransactionsRecord record,
                                 TonTransactionInfo tx,
                                 int confirmedBlocks) {
        boolean updated = txRepository.updateConfirmed(
                record.getId(), tx.txHash(), confirmedBlocks,
                tx.feeNano(),
                OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(tx.utime()),
                        ZoneOffset.UTC),
                record.getVersion());

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
                tx.txHash(), tx.amountNano(), confirmedBlocks,
                fromAddr, toAddr);

        publishOutboxEvent(record,
                EventTypes.DEPOSIT_CONFIRMED, event);

        metrics.incrementCounter(MetricNames.TON_DEPOSIT_CONFIRMED);
        log.info("Deposit confirmed: id={}, txHash={}, "
                        + "amount={}, confirmations={}",
                record.getId(), tx.txHash(),
                tx.amountNano(), confirmedBlocks);
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
        txRepository.updateStatus(record.getId(), "TIMEOUT",
                0, record.getVersion());

        var event = new DepositFailedEvent(
                DepositFailureReason.TIMEOUT,
                record.getAmountNano(), 0L);

        publishOutboxEvent(record,
                EventTypes.DEPOSIT_FAILED, event);

        metrics.incrementCounter(MetricNames.TON_DEPOSIT_TIMEOUT);
        log.info("Deposit timed out: id={}, dealId={}",
                record.getId(), record.getDealId());
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

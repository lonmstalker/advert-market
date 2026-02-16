package com.advertmarket.financial.ton.service;

import com.advertmarket.db.generated.tables.records.TonTransactionsRecord;
import com.advertmarket.financial.api.event.DepositConfirmedEvent;
import com.advertmarket.financial.api.event.DepositFailedEvent;
import com.advertmarket.financial.api.event.DepositFailureReason;
import com.advertmarket.financial.api.model.DepositInfo;
import com.advertmarket.financial.api.model.DepositStatus;
import com.advertmarket.financial.api.model.TonTransactionInfo;
import com.advertmarket.financial.api.port.DepositPort;
import com.advertmarket.financial.api.port.TonBlockchainPort;
import com.advertmarket.financial.config.TonProperties;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.shared.event.DomainEvent;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import com.advertmarket.shared.outbox.OutboxStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

/**
 * Deposit projection and operator-review operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"fenum:argument", "fenum:assignment"})
public class DepositService implements DepositPort {

    private static final int TX_FETCH_LIMIT = 50;

    private final JooqTonTransactionRepository txRepository;
    private final TonBlockchainPort blockchainPort;
    private final ConfirmationPolicyService confirmationPolicyService;
    private final OutboxRepository outboxRepository;
    private final TonProperties.Deposit depositProperties;
    private final JsonFacade jsonFacade;

    @Override
    public @NonNull Optional<DepositInfo> getDepositInfo(@NonNull DealId dealId) {
        return txRepository.findLatestInboundByDealId(dealId.value())
                .map(record -> toDepositInfo(dealId, record));
    }

    @Override
    public void approveDeposit(@NonNull DealId dealId) {
        var record = txRepository.findLatestInboundByDealId(dealId.value())
                .orElseThrow(() -> new DomainException(
                        ErrorCodes.ENTITY_NOT_FOUND,
                        "Deposit transaction not found for deal " + dealId));
        requireReviewState(record);

        var snapshot = snapshot(record);
        var confirmations = record.getConfirmations() != null
                ? record.getConfirmations() : 0;
        var expectedAmount = requiredAmount(record);
        boolean updated = txRepository.updateConfirmed(
                record.getId(),
                snapshot.txHash(),
                confirmations,
                snapshot.feeNano(),
                OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(snapshot.utime()),
                        ZoneOffset.UTC),
                snapshot.fromAddress(),
                version(record));

        if (!updated) {
            log.info("Deposit approve CAS skipped for deal={}", dealId);
            return;
        }

        publishEvent(
                dealId,
                EventTypes.DEPOSIT_CONFIRMED,
                "deposit-review-approve",
                new DepositConfirmedEvent(
                        snapshot.txHash(),
                        snapshot.receivedAmountNano(),
                        expectedAmount,
                        confirmations,
                        snapshot.fromAddress(),
                        requiredToAddress(record)));
    }

    @Override
    public void rejectDeposit(@NonNull DealId dealId) {
        var record = txRepository.findLatestInboundByDealId(dealId.value())
                .orElseThrow(() -> new DomainException(
                        ErrorCodes.ENTITY_NOT_FOUND,
                        "Deposit transaction not found for deal " + dealId));
        requireReviewState(record);

        var received = computeReceivedAmount(record).orElse(0L);
        var expectedAmount = requiredAmount(record);
        boolean updated = txRepository.updateStatus(
                record.getId(),
                "REJECTED",
                confirmations(record),
                version(record));
        if (!updated) {
            log.info("Deposit reject CAS skipped for deal={}", dealId);
            return;
        }

        publishEvent(
                dealId,
                EventTypes.DEPOSIT_FAILED,
                "deposit-review-reject",
                new DepositFailedEvent(
                        DepositFailureReason.REJECTED,
                        expectedAmount,
                        received));
    }

    @Override
    public @NonNull Optional<String> findRefundAddress(@NonNull DealId dealId) {
        return txRepository.findLatestInboundByDealId(dealId.value())
                .map(TonTransactionsRecord::getFromAddress)
                .filter(address -> !address.isBlank());
    }

    private DepositInfo toDepositInfo(DealId dealId, TonTransactionsRecord record) {
        var expectedAmount = requiredAmount(record);
        var receivedAmount = computeReceivedAmount(record);
        var amountForPolicy = receivedAmount.orElse(expectedAmount);
        var required = confirmationPolicyService.requiredConfirmations(amountForPolicy)
                .confirmations();

        return new DepositInfo(
                requiredToAddress(record),
                String.valueOf(expectedAmount),
                dealId.value().toString(),
                toApiStatus(record.getStatus()),
                record.getConfirmations(),
                required,
                receivedAmount.map(String::valueOf).orElse(null),
                record.getTxHash(),
                resolveExpiresAt(record).map(Instant::toString).orElse(null));
    }

    private Optional<Instant> resolveExpiresAt(TonTransactionsRecord record) {
        if (record.getCreatedAt() == null) {
            return Optional.empty();
        }
        var expiresAt = record.getCreatedAt()
                .toInstant()
                .plus(depositProperties.maxPollDuration());
        return Optional.of(expiresAt);
    }

    private static int confirmations(TonTransactionsRecord record) {
        return record.getConfirmations() != null ? record.getConfirmations() : 0;
    }

    private static int version(TonTransactionsRecord record) {
        return record.getVersion() != null ? record.getVersion() : 0;
    }

    private static long requiredAmount(TonTransactionsRecord record) {
        return Objects.requireNonNull(
                record.getAmountNano(),
                "amount_nano must be present for deposit");
    }

    private static String requiredToAddress(TonTransactionsRecord record) {
        return Objects.requireNonNull(
                record.getToAddress(),
                "to_address must be present for deposit");
    }

    private static DepositStatus toApiStatus(String rawStatus) {
        if (rawStatus == null) {
            return DepositStatus.AWAITING_PAYMENT;
        }
        return switch (rawStatus) {
            case "TX_DETECTED" -> DepositStatus.TX_DETECTED;
            case "CONFIRMING" -> DepositStatus.CONFIRMING;
            case "AWAITING_OPERATOR_REVIEW" -> DepositStatus.AWAITING_OPERATOR_REVIEW;
            case "CONFIRMED" -> DepositStatus.CONFIRMED;
            case "TIMEOUT" -> DepositStatus.EXPIRED;
            case "UNDERPAID" -> DepositStatus.UNDERPAID;
            case "OVERPAID" -> DepositStatus.OVERPAID;
            case "REJECTED" -> DepositStatus.REJECTED;
            default -> DepositStatus.AWAITING_PAYMENT;
        };
    }

    private void requireReviewState(TonTransactionsRecord record) {
        if (!"AWAITING_OPERATOR_REVIEW".equals(record.getStatus())) {
            throw new DomainException(
                    ErrorCodes.INVALID_STATE_TRANSITION,
                    "Deposit is not awaiting operator review");
        }
    }

    private DepositSnapshot snapshot(TonTransactionsRecord record) {
        var txs = loadInboundTransactions(record);
        if (txs.isEmpty()) {
            throw new DomainException(
                    ErrorCodes.ENTITY_NOT_FOUND,
                    "No on-chain transactions for deposit address");
        }
        long total = txs.stream().mapToLong(TonTransactionInfo::amountNano).sum();
        var latest = txs.stream()
                .max(Comparator.comparingLong(TonTransactionInfo::lt))
                .orElseThrow();
        return new DepositSnapshot(
                total,
                latest.txHash(),
                Objects.requireNonNullElse(latest.fromAddress(), ""),
                latest.feeNano(),
                latest.utime());
    }

    private Optional<Long> computeReceivedAmount(TonTransactionsRecord record) {
        var txs = loadInboundTransactions(record);
        if (txs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(txs.stream().mapToLong(TonTransactionInfo::amountNano).sum());
    }

    private List<TonTransactionInfo> loadInboundTransactions(TonTransactionsRecord record) {
        var toAddress = record.getToAddress();
        if (toAddress == null || toAddress.isBlank()) {
            return List.of();
        }
        return blockchainPort.getTransactions(toAddress, TX_FETCH_LIMIT)
                .stream()
                .filter(tx -> tx.amountNano() > 0)
                .toList();
    }

    private void publishEvent(
            DealId dealId,
            String eventType,
            String action,
            DomainEvent event) {
        var envelope = EventEnvelope.create(eventType, dealId, event);
        outboxRepository.save(OutboxEntry.builder()
                .dealId(dealId)
                .idempotencyKey("deposit:%s:%s".formatted(dealId.value(), action))
                .topic(TopicNames.FINANCIAL_EVENTS)
                .partitionKey(dealId.value().toString())
                .payload(jsonFacade.toJson(envelope))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .version(0)
                .createdAt(Instant.now())
                .build());
    }

    private record DepositSnapshot(
            long receivedAmountNano,
            String txHash,
            String fromAddress,
            long feeNano,
            long utime) {
    }
}

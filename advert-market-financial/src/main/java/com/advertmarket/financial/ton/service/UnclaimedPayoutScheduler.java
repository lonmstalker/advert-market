package com.advertmarket.financial.ton.service;

import com.advertmarket.communication.api.event.NotificationEvent;
import com.advertmarket.communication.api.notification.NotificationType;
import com.advertmarket.financial.api.event.ExecutePayoutCommand;
import com.advertmarket.financial.config.UnclaimedPayoutProperties;
import com.advertmarket.financial.ton.repository.UnclaimedPayoutRepository;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily reconciliation of payouts that were deferred due missing TON address.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(UnclaimedPayoutProperties.class)
@SuppressWarnings({"fenum:argument", "fenum:assignment"})
public class UnclaimedPayoutScheduler {

    private static final String LOCK_KEY = "scheduler:unclaimed-payout";
    private static final int SHORT_DEAL_ID_LENGTH = 8;

    private final UnclaimedPayoutRepository repository;
    private final OutboxRepository outboxRepository;
    private final DistributedLockPort lockPort;
    private final JsonFacade jsonFacade;
    private final MetricsFacade metrics;
    private final UnclaimedPayoutProperties props;

    /** Runs daily reminder/escalation checks for unclaimed payouts. */
    @Scheduled(cron = "${app.financial.unclaimed-payout.cron:0 30 2 * * *}")
    public void poll() {
        var token = lockPort.tryLock(LOCK_KEY, props.lockTtl());
        if (token.isEmpty()) {
            log.debug("Could not acquire unclaimed payout scheduler lock");
            return;
        }

        try {
            processAt(OffsetDateTime.now(ZoneOffset.UTC));
        } finally {
            lockPort.unlock(LOCK_KEY, token.get());
        }
    }

    void processAt(OffsetDateTime now) {
        var candidates = repository.findOpenPayoutCandidates(props.batchSize());
        if (candidates.isEmpty()) {
            return;
        }

        List<Long> operatorIds = List.of();
        for (var candidate : candidates) {
            if (candidate.hasTonAddress()) {
                emitRetryPayout(candidate);
                continue;
            }

            int days = daysSinceCompletion(candidate, now);
            emitReachedReminders(candidate, days);

            if (days >= props.operatorReviewDay()) {
                if (operatorIds.isEmpty()) {
                    operatorIds = repository.findOperatorUserIds();
                }
                emitOperatorReview(candidate, operatorIds);
            }
        }
    }

    private static int daysSinceCompletion(
            UnclaimedPayoutCandidate candidate,
            OffsetDateTime now) {
        long days = ChronoUnit.DAYS.between(
                candidate.completedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
                now.toLocalDate());
        return (int) Math.max(0L, days);
    }

    private void emitRetryPayout(UnclaimedPayoutCandidate candidate) {
        if (candidate.subwalletId() <= 0) {
            log.warn("Unclaimed payout retry skipped, invalid subwallet: deal={}",
                    candidate.dealId());
            return;
        }

        var payload = new ExecutePayoutCommand(
                candidate.ownerId(),
                candidate.amountNano(),
                candidate.commissionNano(),
                candidate.subwalletId());
        var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_PAYOUT,
                DealId.of(candidate.dealId()),
                payload);
        saveOutboxIgnoringDuplicate(OutboxEntry.builder()
                .dealId(DealId.of(candidate.dealId()))
                .idempotencyKey(
                        "unclaimed-payout:retry:%s".formatted(candidate.dealId()))
                .topic(TopicNames.FINANCIAL_COMMANDS)
                .partitionKey(candidate.dealId().toString())
                .payload(jsonFacade.toJson(envelope))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .version(0)
                .createdAt(Instant.now())
                .build());
        metrics.incrementCounter(MetricNames.PAYOUT_UNCLAIMED_RETRY_EMITTED);
    }

    private void emitReachedReminders(
            UnclaimedPayoutCandidate candidate,
            int daysSinceCompletion) {
        for (Integer threshold : props.reminderDays()) {
            if (threshold == null || threshold < 0
                    || daysSinceCompletion < threshold) {
                continue;
            }
            emitOwnerReminder(candidate, threshold);
        }
    }

    private void emitOwnerReminder(
            UnclaimedPayoutCandidate candidate,
            int reminderDay) {
        var payload = new NotificationEvent(
                candidate.ownerId(),
                NotificationType.PAYOUT_UNCLAIMED.name(),
                props.notificationLocale(),
                Map.of(
                        "deal_id_short", shortDealId(candidate),
                        "amount", String.valueOf(candidate.amountNano()),
                        "days", String.valueOf(reminderDay)),
                null);
        var envelope = EventEnvelope.create(
                EventTypes.NOTIFICATION,
                DealId.of(candidate.dealId()),
                payload);
        saveOutboxIgnoringDuplicate(OutboxEntry.builder()
                .dealId(DealId.of(candidate.dealId()))
                .idempotencyKey(
                        "unclaimed-payout:reminder:%s:%d"
                                .formatted(candidate.dealId(), reminderDay))
                .topic(TopicNames.COMMUNICATION_NOTIFICATIONS)
                .partitionKey(String.valueOf(candidate.ownerId()))
                .payload(jsonFacade.toJson(envelope))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .version(0)
                .createdAt(Instant.now())
                .build());
        metrics.incrementCounter(MetricNames.PAYOUT_UNCLAIMED_REMINDER);
    }

    private void emitOperatorReview(
            UnclaimedPayoutCandidate candidate,
            List<Long> operatorIds) {
        for (Long operatorId : operatorIds) {
            if (operatorId == null) {
                continue;
            }
            var payload = new NotificationEvent(
                    operatorId,
                    NotificationType.PAYOUT_UNCLAIMED_OPERATOR_REVIEW.name(),
                    props.notificationLocale(),
                    Map.of(
                            "deal_id_short", shortDealId(candidate),
                            "owner_id", String.valueOf(candidate.ownerId()),
                            "amount", String.valueOf(candidate.amountNano()),
                            "days", String.valueOf(props.operatorReviewDay())),
                    null);
            var envelope = EventEnvelope.create(
                    EventTypes.NOTIFICATION,
                    DealId.of(candidate.dealId()),
                    payload);
            saveOutboxIgnoringDuplicate(OutboxEntry.builder()
                    .dealId(DealId.of(candidate.dealId()))
                    .idempotencyKey(
                            "unclaimed-payout:operator-review:%s:%d:%d"
                                    .formatted(
                                            candidate.dealId(),
                                            operatorId,
                                            props.operatorReviewDay()))
                    .topic(TopicNames.COMMUNICATION_NOTIFICATIONS)
                    .partitionKey(String.valueOf(operatorId))
                    .payload(jsonFacade.toJson(envelope))
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .version(0)
                    .createdAt(Instant.now())
                    .build());
            metrics.incrementCounter(
                    MetricNames.PAYOUT_UNCLAIMED_OPERATOR_ESCALATED);
        }
    }

    private void saveOutboxIgnoringDuplicate(OutboxEntry entry) {
        try {
            outboxRepository.save(entry);
        } catch (DataIntegrityViolationException exception) {
            log.debug("Duplicate unclaimed payout outbox idempotency key, skip");
        }
    }

    private static String shortDealId(UnclaimedPayoutCandidate candidate) {
        return candidate.dealId()
                .toString()
                .substring(0, SHORT_DEAL_ID_LENGTH);
    }
}

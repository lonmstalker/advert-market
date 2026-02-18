package com.advertmarket.deal.service;

import com.advertmarket.communication.api.event.NotificationEvent;
import com.advertmarket.communication.api.notification.NotificationType;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.deal.config.DealTimeoutProperties;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import com.advertmarket.shared.outbox.OutboxStatus;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls for deals that have exceeded their deadline and transitions
 * them to EXPIRED status.
 *
 * <p>Uses distributed locking to prevent concurrent processing
 * across multiple instances. Funded deals emit a refund indication
 * via the transition reason.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"fenum:argument", "fenum:assignment"})
public class DealTimeoutScheduler {

    private static final String SCHEDULER_LOCK_KEY = "scheduler:deal-timeout";
    private static final String DEAL_LOCK_PREFIX = "lock:deal:";
    private static final String TIMEOUT_REASON = "Deal expired due to timeout";
    private static final int SHORT_DEAL_ID_LENGTH = 8;
    private static final long SYSTEM_ACTOR_ID = 0L;

    private final DealRepository dealRepository;
    private final DealTransitionService dealTransitionService;
    private final DistributedLockPort lockPort;
    private final OutboxRepository outboxRepository;
    private final JsonFacade jsonFacade;
    private final MetricsFacade metrics;
    private final DealTimeoutProperties props;

    /** Polls for expired deals and transitions them to EXPIRED status. */
    @Scheduled(fixedDelayString = "${app.deal.timeout.poll-interval:60000}")
    public void processExpiredDeals() {
        var token = lockPort.tryLock(SCHEDULER_LOCK_KEY, props.lockTtl());
        if (token.isEmpty()) {
            log.debug("Could not acquire timeout scheduler lock, skipping");
            return;
        }

        try {
            doProcessExpiredDeals();
        } finally {
            lockPort.unlock(SCHEDULER_LOCK_KEY, token.get());
        }
    }

    private void doProcessExpiredDeals() {
        var expiredDeals = dealRepository.findExpiredDeals(
                props.batchSize(),
                props.gracePeriod());
        if (expiredDeals.isEmpty()) {
            return;
        }

        log.info("Processing {} expired deals", expiredDeals.size());

        for (var deal : expiredDeals) {
            processOne(deal);
        }
    }

    private void processOne(DealRecord staleDeal) {
        var dealId = DealId.of(staleDeal.id());
        var dealLockKey = DEAL_LOCK_PREFIX + staleDeal.id();
        var dealLockToken = lockPort.tryLock(dealLockKey, props.lockTtl());
        if (dealLockToken.isEmpty()) {
            log.debug("Could not acquire deal lock for {}, skipping", dealId);
            return;
        }

        try {
            processLocked(dealId);
        } catch (Exception ex) {
            log.warn("Failed to process timeout for deal={}: {}", dealId, ex.getMessage());
        } finally {
            lockPort.unlock(dealLockKey, dealLockToken.get());
        }
    }

    private void processLocked(DealId dealId) {
        var current = dealRepository.findById(dealId).orElse(null);
        if (current == null) {
            log.debug("Deal not found during timeout processing: {}", dealId);
            return;
        }

        if (!isExpiredCandidate(current)) {
            log.debug("Deal {} is no longer expired candidate, skipping", dealId);
            return;
        }

        if (current.status() == DealStatus.DISPUTED) {
            escalateDispute(dealId, current);
            return;
        }

        var targetStatus = timeoutTarget(current.status());
        if (targetStatus == null) {
            log.debug("No timeout action configured for status {}, deal={}",
                    current.status(), dealId);
            return;
        }

        var command = new DealTransitionCommand(
                dealId,
                targetStatus,
                SYSTEM_ACTOR_ID,
                ActorType.SYSTEM,
                TIMEOUT_REASON,
                null,
                null);
        var result = dealTransitionService.transition(command);

        if (result instanceof DealTransitionResult.Success
                || result instanceof DealTransitionResult.AlreadyInTargetState) {
            dealRepository.clearDeadline(dealId);
            metrics.incrementCounter(
                    MetricNames.DEAL_TIMEOUT_PROCESSED,
                    "from_status", current.status().name(),
                    "to_status", targetStatus.name());

            if (current.status().isFunded() && targetStatus == DealStatus.EXPIRED) {
                metrics.incrementCounter(
                        MetricNames.DEAL_TIMEOUT_REFUND_EMITTED,
                        "from_status", current.status().name());
            }

            log.info("Timeout processed: deal={} {} -> {}",
                    dealId, current.status(), targetStatus);
        }
    }

    private void escalateDispute(DealId dealId, DealRecord current) {
        var operatorIds = dealRepository.findOperatorUserIds();
        for (Long operatorId : operatorIds) {
            publishOperatorEscalation(dealId, current, operatorId);
        }
        dealRepository.clearDeadline(dealId);
        metrics.incrementCounter(
                MetricNames.DEAL_TIMEOUT_DISPUTE_ESCALATED,
                "operators", String.valueOf(operatorIds.size()));
        log.info(
                "Dispute timeout escalated: deal={}, operators={}",
                dealId,
                operatorIds.size());
    }

    private void publishOperatorEscalation(
            DealId dealId,
            DealRecord deal,
            long operatorId) {
        var payload = new NotificationEvent(
                operatorId,
                NotificationType.RECONCILIATION_ALERT.name(),
                "ru",
                Map.of(
                        "deal_id_short",
                        deal.id().toString().substring(0, SHORT_DEAL_ID_LENGTH),
                        "channel_id",
                        String.valueOf(deal.channelId())),
                null);
        var envelope = EventEnvelope.create(
                EventTypes.NOTIFICATION,
                dealId,
                payload);
        outboxRepository.save(OutboxEntry.builder()
                .dealId(dealId)
                .idempotencyKey(
                        "deal-timeout:disputed-escalation:%s:%d"
                                .formatted(deal.id(), operatorId))
                .topic(TopicNames.COMMUNICATION_NOTIFICATIONS)
                .partitionKey(String.valueOf(operatorId))
                .payload(jsonFacade.toJson(envelope))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .version(0)
                .createdAt(Instant.now())
                .build());
    }

    private boolean isExpiredCandidate(DealRecord deal) {
        if (deal.status().isTerminal()) {
            return false;
        }
        var deadlineAt = deal.deadlineAt();
        if (deadlineAt == null) {
            return false;
        }
        var deadlineThreshold = java.time.Instant.now().minus(props.gracePeriod());
        return !deadlineAt.isAfter(deadlineThreshold);
    }

    private @Nullable DealStatus timeoutTarget(DealStatus status) {
        return switch (status) {
            case OFFER_PENDING,
                    NEGOTIATING,
                    AWAITING_PAYMENT,
                    FUNDED,
                    CREATIVE_SUBMITTED,
                    CREATIVE_APPROVED,
                    SCHEDULED -> DealStatus.EXPIRED;
            case DELIVERY_VERIFYING -> DealStatus.DISPUTED;
            default -> null;
        };
    }
}

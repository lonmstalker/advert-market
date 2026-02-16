package com.advertmarket.deal.service;

import com.advertmarket.communication.api.event.NotificationEvent;
import com.advertmarket.communication.api.notification.NotificationType;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.event.DealStateChangedEvent;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.delivery.api.event.CreativeDraft;
import com.advertmarket.delivery.api.event.PublishPostCommand;
import com.advertmarket.delivery.api.event.VerifyDeliveryCommand;
import com.advertmarket.financial.api.event.ExecutePayoutCommand;
import com.advertmarket.financial.api.event.ExecuteRefundCommand;
import com.advertmarket.financial.api.event.WatchDepositCommand;
import com.advertmarket.financial.api.port.DepositPort;
import com.advertmarket.financial.api.port.EscrowPort;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.shared.event.DomainEvent;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import com.advertmarket.shared.outbox.OutboxStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Executes post-transition side effects for deal state changes.
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"fenum:argument", "fenum:assignment"})
public class DealWorkflowEngine {

    private static final Duration OFFER_PENDING_TIMEOUT = Duration.ofHours(48);
    private static final Duration NEGOTIATING_TIMEOUT = Duration.ofHours(72);
    private static final Duration AWAITING_PAYMENT_TIMEOUT = Duration.ofHours(24);
    private static final Duration FUNDED_TIMEOUT = Duration.ofHours(72);
    private static final Duration CREATIVE_APPROVED_TIMEOUT = Duration.ofHours(48);
    private static final Duration SCHEDULED_TIMEOUT = Duration.ofHours(24);
    private static final Duration DELIVERY_VERIFYING_TIMEOUT = Duration.ofHours(24);
    private static final Duration DISPUTE_TIMEOUT = Duration.ofDays(7);

    private final DealRepository dealRepository;
    private final DealTransitionService dealTransitionService;
    private final EscrowPort escrowPort;
    private final DepositPort depositPort;
    private final OutboxRepository outboxRepository;
    private final ChannelRepository channelRepository;
    private final JsonFacade jsonFacade;

    /**
     * Handles one DEAL_STATE_CHANGED event.
     *
     * @param envelope state-changed envelope
     */
    public void handle(@NonNull EventEnvelope<DealStateChangedEvent> envelope) {
        var dealId = Objects.requireNonNull(
                envelope.dealId(), "dealId required for workflow");
        var event = envelope.payload();
        var deal = dealRepository.findById(dealId).orElse(null);
        if (deal == null) {
            log.warn("Workflow skipped: deal not found for {}", dealId);
            return;
        }

        applyDeadline(dealId, event.toStatus());
        routeByTargetStatus(envelope, deal);
    }

    private void routeByTargetStatus(
            EventEnvelope<DealStateChangedEvent> envelope,
            DealRecord deal) {
        var event = envelope.payload();
        switch (event.toStatus()) {
            case OFFER_PENDING -> {
                notifyOne(
                        envelope,
                        deal,
                        deal.ownerId(),
                        NotificationType.NEW_OFFER,
                        "offer-pending-owner");
            }
            case NEGOTIATING -> notifyCounterparty(envelope, deal);
            case ACCEPTED -> notifyOne(
                    envelope,
                    deal,
                    deal.advertiserId(),
                    NotificationType.OFFER_ACCEPTED,
                    "accepted-advertiser");
            case AWAITING_PAYMENT -> onAwaitingPayment(envelope, deal);
            case FUNDED -> notifyOne(
                    envelope,
                    deal,
                    deal.ownerId(),
                    NotificationType.ESCROW_FUNDED,
                    "funded-owner");
            case CREATIVE_SUBMITTED -> notifyOne(
                    envelope,
                    deal,
                    deal.advertiserId(),
                    NotificationType.CREATIVE_SUBMITTED,
                    "creative-submitted-advertiser");
            case CREATIVE_APPROVED -> notifyOne(
                    envelope,
                    deal,
                    deal.ownerId(),
                    NotificationType.CREATIVE_APPROVED,
                    "creative-approved-owner");
            case SCHEDULED -> emitPublishCommand(envelope, deal);
            case PUBLISHED -> onPublished(envelope, deal);
            case DELIVERY_VERIFYING -> {
                // Deadline only.
            }
            case COMPLETED_RELEASED -> onCompletedReleased(envelope, deal);
            case DISPUTED -> notifyBoth(
                    envelope, deal,
                    NotificationType.DISPUTE_OPENED,
                    "disputed-both");
            case REFUNDED -> onRefunded(envelope, deal, deal.amountNano());
            case PARTIALLY_REFUNDED -> onPartiallyRefunded(envelope, deal);
            case CANCELLED, EXPIRED -> onCancelledOrExpired(envelope, deal);
            case DRAFT -> {
                // no-op
            }
        }
    }

    private void onAwaitingPayment(
            EventEnvelope<DealStateChangedEvent> envelope,
            DealRecord deal) {
        var dealId = DealId.of(deal.id());
        String depositAddress = deal.depositAddress();
        int subwalletId = deal.subwalletId() != null
                ? deal.subwalletId() : 0;

        if (depositAddress == null || depositAddress.isBlank()) {
            var info = escrowPort.generateDepositAddress(dealId, deal.amountNano());
            depositAddress = info.depositAddress();
            subwalletId = (int) info.subwalletId();
            dealRepository.setDepositAddress(
                    dealId,
                    depositAddress,
                    subwalletId);
        }

        emitFinancialCommand(
                envelope,
                "watch-deposit",
                EventTypes.WATCH_DEPOSIT,
                new WatchDepositCommand(
                        depositAddress,
                        deal.amountNano(),
                        deal.advertiserId()));
    }

    private void onPublished(
            EventEnvelope<DealStateChangedEvent> envelope,
            DealRecord deal) {
        if (deal.messageId() != null
                && deal.contentHash() != null
                && deal.publishedAt() != null) {
            emitDeliveryCommand(
                    envelope,
                    "verify-delivery",
                    EventTypes.VERIFY_DELIVERY,
                    new VerifyDeliveryCommand(
                            deal.channelId(),
                            deal.messageId(),
                            deal.contentHash(),
                            deal.publishedAt(),
                            1));
        } else {
            log.warn("Cannot emit VERIFY_DELIVERY for deal={} due to missing metadata",
                    deal.id());
        }

        dealTransitionService.transition(new DealTransitionCommand(
                DealId.of(deal.id()),
                DealStatus.DELIVERY_VERIFYING,
                null,
                ActorType.SYSTEM,
                "Auto transition after publication",
                null,
                null));
    }

    private void onCompletedReleased(
            EventEnvelope<DealStateChangedEvent> envelope,
            DealRecord deal) {
        var dealId = DealId.of(deal.id());
        escrowPort.releaseEscrow(
                dealId,
                new UserId(deal.ownerId()),
                deal.amountNano(),
                deal.commissionRateBp());

        int subwallet = requiredSubwallet(deal);
        long payoutAmount = deal.amountNano() - deal.commissionNano();
        emitFinancialCommand(
                envelope,
                "execute-payout",
                EventTypes.EXECUTE_PAYOUT,
                new ExecutePayoutCommand(
                        deal.ownerId(),
                        payoutAmount,
                        deal.commissionNano(),
                        subwallet));

        notifyOne(
                envelope, deal, deal.ownerId(),
                NotificationType.PAYOUT_SENT,
                "completed-owner");
        notifyOne(
                envelope, deal, deal.advertiserId(),
                NotificationType.DELIVERY_VERIFIED,
                "completed-advertiser");
    }

    private void onRefunded(
            EventEnvelope<DealStateChangedEvent> envelope,
            DealRecord deal,
            long refundAmountNano) {
        if (deal.refundedTxHash() == null || deal.refundedTxHash().isBlank()) {
            emitRefundCommand(envelope, deal, refundAmountNano, "execute-refund");
        }
        notifyBoth(
                envelope, deal,
                NotificationType.DISPUTE_RESOLVED,
                "refunded-both");
    }

    private void onPartiallyRefunded(
            EventEnvelope<DealStateChangedEvent> envelope,
            DealRecord deal) {
        var event = envelope.payload();
        var partialRefund = Objects.requireNonNull(
                event.partialRefundNano(),
                "partialRefundNano required for PARTIALLY_REFUNDED");
        var partialPayout = Objects.requireNonNull(
                event.partialPayoutNano(),
                "partialPayoutNano required for PARTIALLY_REFUNDED");

        escrowPort.releaseEscrow(
                DealId.of(deal.id()),
                new UserId(deal.ownerId()),
                partialPayout,
                0);

        emitRefundCommand(envelope, deal, partialRefund, "execute-refund-partial");
        emitFinancialCommand(
                envelope,
                "execute-payout-partial",
                EventTypes.EXECUTE_PAYOUT,
                new ExecutePayoutCommand(
                        deal.ownerId(),
                        partialPayout,
                        0L,
                        requiredSubwallet(deal)));
        notifyBoth(
                envelope, deal,
                NotificationType.DISPUTE_RESOLVED,
                "partial-refunded-both");
    }

    private void onCancelledOrExpired(
            EventEnvelope<DealStateChangedEvent> envelope,
            DealRecord deal) {
        var event = envelope.payload();
        if (event.fromStatus().isFunded()) {
            emitRefundCommand(envelope, deal, deal.amountNano(), "execute-refund-cancel-expire");
        }

        var type = event.toStatus() == DealStatus.CANCELLED
                ? NotificationType.DEAL_CANCELLED
                : NotificationType.DEAL_EXPIRED;
        notifyBoth(envelope, deal, type, "cancelled-or-expired-both");
    }

    private void emitPublishCommand(
            EventEnvelope<DealStateChangedEvent> envelope,
            DealRecord deal) {
        var command = new PublishPostCommand(
                deal.channelId(),
                toCreativeDraft(deal.creativeDraft()),
                null);
        emitDeliveryCommand(
                envelope,
                "publish-post",
                EventTypes.PUBLISH_POST,
                command);
    }

    private void notifyCounterparty(
            EventEnvelope<DealStateChangedEvent> envelope,
            DealRecord deal) {
        var actorType = envelope.payload().actorType();
        long recipient = actorType == ActorType.ADVERTISER
                ? deal.ownerId()
                : deal.advertiserId();
        notifyOne(
                envelope,
                deal,
                recipient,
                NotificationType.NEW_OFFER,
                "negotiating-counterparty");
    }

    private void notifyBoth(
            EventEnvelope<DealStateChangedEvent> envelope,
            DealRecord deal,
            NotificationType type,
            String action) {
        notifyOne(envelope, deal, deal.advertiserId(), type, action + "-advertiser");
        notifyOne(envelope, deal, deal.ownerId(), type, action + "-owner");
    }

    private void notifyOne(
            EventEnvelope<DealStateChangedEvent> envelope,
            DealRecord deal,
            long recipientId,
            NotificationType type,
            String action) {
        var payload = new NotificationEvent(
                recipientId,
                type.name(),
                "ru",
                notificationVars(deal),
                null);
        var event = EventEnvelope.create(
                EventTypes.NOTIFICATION,
                DealId.of(deal.id()),
                payload);
        saveOutbox(
                envelope,
                action + "-" + type.name() + "-" + recipientId,
                TopicNames.COMMUNICATION_NOTIFICATIONS,
                event);
    }

    private void emitFinancialCommand(
            EventEnvelope<DealStateChangedEvent> sourceEnvelope,
            String action,
            String eventType,
            DomainEvent payload) {
        var envelope = EventEnvelope.create(
                eventType, sourceEnvelope.dealId(), payload);
        saveOutbox(
                sourceEnvelope,
                action + "-" + eventType,
                TopicNames.FINANCIAL_COMMANDS,
                envelope);
    }

    private void emitDeliveryCommand(
            EventEnvelope<DealStateChangedEvent> sourceEnvelope,
            String action,
            String eventType,
            DomainEvent payload) {
        var envelope = EventEnvelope.create(
                eventType, sourceEnvelope.dealId(), payload);
        saveOutbox(
                sourceEnvelope,
                action + "-" + eventType,
                TopicNames.DELIVERY_COMMANDS,
                envelope);
    }

    private void emitRefundCommand(
            EventEnvelope<DealStateChangedEvent> sourceEnvelope,
            DealRecord deal,
            long amountNano,
            String action) {
        var refundAddress = depositPort.findRefundAddress(DealId.of(deal.id()));
        if (refundAddress.isEmpty()) {
            log.warn("Refund command skipped: no refund source address for deal={}", deal.id());
            return;
        }

        emitFinancialCommand(
                sourceEnvelope,
                action,
                EventTypes.EXECUTE_REFUND,
                new ExecuteRefundCommand(
                        deal.advertiserId(),
                        amountNano,
                        refundAddress.get(),
                        requiredSubwallet(deal)));
    }

    private void saveOutbox(
            EventEnvelope<DealStateChangedEvent> sourceEnvelope,
            String action,
            String topic,
            EventEnvelope<?> payload) {
        var now = Instant.now();
        var dealId = sourceEnvelope.dealId();
        var outbox = OutboxEntry.builder()
                .dealId(dealId)
                .idempotencyKey("workflow:%s:%s"
                        .formatted(sourceEnvelope.eventId(), action))
                .topic(topic)
                .partitionKey(dealId != null ? dealId.value().toString() : null)
                .payload(jsonFacade.toJson(payload))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .version(0)
                .createdAt(now)
                .build();
        outboxRepository.save(outbox);
    }

    private static int requiredSubwallet(DealRecord deal) {
        if (deal.subwalletId() == null) {
            throw new IllegalStateException(
                    "subwalletId is required for deal " + deal.id());
        }
        return deal.subwalletId();
    }

    private void applyDeadline(DealId dealId, DealStatus toStatus) {
        var timeout = deadlineFor(toStatus);
        if (timeout.isPresent()) {
            dealRepository.setDeadline(dealId, Instant.now().plus(timeout.get()));
            return;
        }
        dealRepository.clearDeadline(dealId);
    }

    private static Optional<Duration> deadlineFor(DealStatus status) {
        return switch (status) {
            case OFFER_PENDING -> Optional.of(OFFER_PENDING_TIMEOUT);
            case NEGOTIATING -> Optional.of(NEGOTIATING_TIMEOUT);
            case AWAITING_PAYMENT -> Optional.of(AWAITING_PAYMENT_TIMEOUT);
            case FUNDED -> Optional.of(FUNDED_TIMEOUT);
            case CREATIVE_APPROVED -> Optional.of(CREATIVE_APPROVED_TIMEOUT);
            case SCHEDULED -> Optional.of(SCHEDULED_TIMEOUT);
            case DELIVERY_VERIFYING -> Optional.of(DELIVERY_VERIFYING_TIMEOUT);
            case DISPUTED -> Optional.of(DISPUTE_TIMEOUT);
            default -> Optional.empty();
        };
    }

    private Map<String, String> notificationVars(DealRecord deal) {
        String channelName = channelRepository.findDetailById(deal.channelId())
                .map(c -> c.title())
                .orElse("channel");
        String shortId = deal.id().toString().substring(0, 8);
        return Map.of(
                "channel_name", channelName,
                "deal_id_short", shortId,
                "amount", String.valueOf(deal.amountNano()));
    }

    private CreativeDraft toCreativeDraft(String creativeDraftJson) {
        if (creativeDraftJson == null || creativeDraftJson.isBlank()) {
            return new CreativeDraft("", List.of(), List.of());
        }
        try {
            var node = jsonFacade.readTree(creativeDraftJson);
            String text = node.path("text").asText("");
            return new CreativeDraft(text, List.of(), List.of());
        } catch (RuntimeException ex) {
            return new CreativeDraft("", List.of(), List.of());
        }
    }
}

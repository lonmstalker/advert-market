package com.advertmarket.deal.service;

import com.advertmarket.deal.api.dto.DealEventRecord;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.api.event.DealStateChangedEvent;
import com.advertmarket.deal.api.port.DealEventRepository;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.exception.InvalidStateTransitionException;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import com.advertmarket.shared.outbox.OutboxStatus;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Pure domain service implementing the deal state machine.
 *
 * <p>Validates transitions, enforces actor permissions,
 * uses optimistic locking (CAS), and writes event + outbox entries.
 */
@RequiredArgsConstructor
public class DealTransitionService {

    private record TransitionKey(DealStatus from, DealStatus to) {
    }

    private static final Map<DealStatus, Set<DealStatus>> TRANSITIONS = buildTransitions();
    private static final Map<TransitionKey, Set<ActorType>> ACTOR_PERMISSIONS =
            buildActorPermissions();

    private final DealRepository dealRepository;
    private final DealEventRepository dealEventRepository;
    private final OutboxRepository outboxRepository;
    private final JsonFacade jsonFacade;

    /**
     * Transitions a deal to the requested target status.
     */
    @NonNull
    public DealTransitionResult transition(@NonNull DealTransitionCommand command) {
        var dealId = command.dealId();
        var targetStatus = command.targetStatus();

        var deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCodes.DEAL_NOT_FOUND, "Deal", dealId.value().toString()));

        // Idempotency: already in target state
        if (deal.status() == targetStatus) {
            return new DealTransitionResult.AlreadyInTargetState(deal.status());
        }

        // Validate transition is allowed in the graph
        validateTransition(deal.status(), targetStatus);

        // Validate actor permission
        validateActorPermission(deal.status(), targetStatus, command.actorType());

        // CAS update
        int updated = dealRepository.updateStatus(
                dealId, deal.status(), targetStatus, deal.version());

        if (updated == 0) {
            // Concurrent modification — re-read for idempotency check
            return handleCasConflict(dealId, targetStatus);
        }

        var now = Instant.now();

        // Persist cancellation reason if provided
        var reason = command.reason();
        if (targetStatus == DealStatus.CANCELLED && reason != null) {
            dealRepository.setCancellationReason(dealId, reason);
        }

        // Append event
        appendEvent(deal, targetStatus, command, now);

        // Outbox entry
        publishOutboxEvent(deal, targetStatus, command, now);

        return new DealTransitionResult.Success(targetStatus);
    }

    private DealTransitionResult handleCasConflict(DealId dealId,
                                                    DealStatus targetStatus) {
        var current = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCodes.DEAL_NOT_FOUND, "Deal", dealId.value().toString()));

        if (current.status() == targetStatus) {
            return new DealTransitionResult.AlreadyInTargetState(current.status());
        }

        // Different state — the transition from original state is no longer valid
        throw new InvalidStateTransitionException(
                "Deal", current.status().name(), targetStatus.name());
    }

    private void validateTransition(DealStatus from, DealStatus to) {
        var allowed = TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new InvalidStateTransitionException("Deal", from.name(), to.name());
        }
    }

    private void validateActorPermission(DealStatus from, DealStatus to,
                                          ActorType actorType) {
        var key = new TransitionKey(from, to);
        var allowedActors = ACTOR_PERMISSIONS.get(key);
        if (allowedActors == null || !allowedActors.contains(actorType)) {
            throw new DomainException(ErrorCodes.DEAL_ACTOR_NOT_ALLOWED,
                    "Actor %s cannot perform transition %s → %s"
                            .formatted(actorType, from, to));
        }
    }

    // DealStatus.name() → String is intentional for event persistence
    @SuppressWarnings("fenum:argument")
    private void appendEvent(DealRecord deal, DealStatus targetStatus,
                              DealTransitionCommand command, Instant now) {
        String payload = command.reason() != null
                ? jsonFacade.toJson(Map.of("reason", command.reason()))
                : "{}";
        var event = new DealEventRecord(
                null,
                deal.id(),
                EventTypes.DEAL_STATE_CHANGED,
                deal.status().name(),
                targetStatus.name(),
                command.actorId(),
                command.actorType().name(),
                payload,
                now);
        dealEventRepository.append(event);
    }

    // DealStatus/ActorType.name() → String is intentional for outbox serialization
    @SuppressWarnings({"fenum:argument", "fenum:assignment"})
    private void publishOutboxEvent(DealRecord deal, DealStatus targetStatus,
                                     DealTransitionCommand command, Instant now) {
        var dealId = DealId.of(deal.id());
        var payload = new DealStateChangedEvent(
                deal.status(), targetStatus,
                command.actorId(), command.actorType(),
                deal.amountNano(), deal.channelId());

        var envelope = EventEnvelope.create(
                EventTypes.DEAL_STATE_CHANGED, dealId, payload);

        var outbox = OutboxEntry.builder()
                .dealId(dealId)
                .topic(TopicNames.DEAL_STATE_CHANGED)
                .partitionKey(dealId.value().toString())
                .payload(jsonFacade.toJson(envelope))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .version(0)
                .createdAt(now)
                .build();

        outboxRepository.save(outbox);
    }

    private static Map<DealStatus, Set<DealStatus>> buildTransitions() {
        var map = new EnumMap<DealStatus, Set<DealStatus>>(DealStatus.class);
        map.put(DealStatus.DRAFT, EnumSet.of(
                DealStatus.OFFER_PENDING, DealStatus.CANCELLED));
        map.put(DealStatus.OFFER_PENDING, EnumSet.of(
                DealStatus.NEGOTIATING, DealStatus.ACCEPTED,
                DealStatus.CANCELLED, DealStatus.EXPIRED));
        map.put(DealStatus.NEGOTIATING, EnumSet.of(
                DealStatus.ACCEPTED, DealStatus.CANCELLED, DealStatus.EXPIRED));
        map.put(DealStatus.ACCEPTED, EnumSet.of(
                DealStatus.AWAITING_PAYMENT, DealStatus.CANCELLED));
        map.put(DealStatus.AWAITING_PAYMENT, EnumSet.of(
                DealStatus.FUNDED, DealStatus.CANCELLED, DealStatus.EXPIRED));
        map.put(DealStatus.FUNDED, EnumSet.of(
                DealStatus.CREATIVE_SUBMITTED,
                DealStatus.CANCELLED, DealStatus.EXPIRED));
        map.put(DealStatus.CREATIVE_SUBMITTED, EnumSet.of(
                DealStatus.CREATIVE_APPROVED, DealStatus.FUNDED,
                DealStatus.DISPUTED));
        map.put(DealStatus.CREATIVE_APPROVED, EnumSet.of(
                DealStatus.SCHEDULED, DealStatus.PUBLISHED,
                DealStatus.CANCELLED, DealStatus.EXPIRED));
        map.put(DealStatus.SCHEDULED, EnumSet.of(
                DealStatus.PUBLISHED, DealStatus.CANCELLED, DealStatus.EXPIRED));
        map.put(DealStatus.PUBLISHED, EnumSet.of(
                DealStatus.DELIVERY_VERIFYING));
        map.put(DealStatus.DELIVERY_VERIFYING, EnumSet.of(
                DealStatus.COMPLETED_RELEASED, DealStatus.DISPUTED));
        map.put(DealStatus.DISPUTED, EnumSet.of(
                DealStatus.COMPLETED_RELEASED, DealStatus.REFUNDED,
                DealStatus.PARTIALLY_REFUNDED));
        // Terminal states have no outgoing transitions
        return Map.copyOf(map);
    }

    // CHECKSTYLE.SUPPRESS: MethodLength for +90 lines
    @SuppressWarnings("fenum:assignment")
    private static Map<TransitionKey, Set<ActorType>> buildActorPermissions() {
        var map = new java.util.HashMap<TransitionKey, Set<ActorType>>();

        // DRAFT
        map.put(new TransitionKey(DealStatus.DRAFT, DealStatus.OFFER_PENDING),
                EnumSet.of(ActorType.ADVERTISER));
        map.put(new TransitionKey(DealStatus.DRAFT, DealStatus.CANCELLED),
                EnumSet.of(ActorType.ADVERTISER));

        // OFFER_PENDING
        map.put(new TransitionKey(DealStatus.OFFER_PENDING, DealStatus.NEGOTIATING),
                EnumSet.of(ActorType.CHANNEL_OWNER, ActorType.CHANNEL_ADMIN));
        map.put(new TransitionKey(DealStatus.OFFER_PENDING, DealStatus.ACCEPTED),
                EnumSet.of(ActorType.CHANNEL_OWNER, ActorType.CHANNEL_ADMIN));
        map.put(new TransitionKey(DealStatus.OFFER_PENDING, DealStatus.CANCELLED),
                EnumSet.of(
                        ActorType.ADVERTISER,
                        ActorType.CHANNEL_OWNER,
                        ActorType.CHANNEL_ADMIN));
        map.put(new TransitionKey(DealStatus.OFFER_PENDING, DealStatus.EXPIRED),
                EnumSet.of(ActorType.SYSTEM));

        // NEGOTIATING
        map.put(new TransitionKey(DealStatus.NEGOTIATING, DealStatus.ACCEPTED),
                EnumSet.of(ActorType.CHANNEL_OWNER, ActorType.CHANNEL_ADMIN));
        map.put(new TransitionKey(DealStatus.NEGOTIATING, DealStatus.CANCELLED),
                EnumSet.of(
                        ActorType.ADVERTISER,
                        ActorType.CHANNEL_OWNER,
                        ActorType.CHANNEL_ADMIN));
        map.put(new TransitionKey(DealStatus.NEGOTIATING, DealStatus.EXPIRED),
                EnumSet.of(ActorType.SYSTEM));

        // ACCEPTED
        map.put(new TransitionKey(DealStatus.ACCEPTED, DealStatus.AWAITING_PAYMENT),
                EnumSet.of(ActorType.SYSTEM));
        map.put(new TransitionKey(DealStatus.ACCEPTED, DealStatus.CANCELLED),
                EnumSet.of(
                        ActorType.ADVERTISER,
                        ActorType.CHANNEL_OWNER,
                        ActorType.CHANNEL_ADMIN));

        // AWAITING_PAYMENT
        map.put(new TransitionKey(DealStatus.AWAITING_PAYMENT, DealStatus.FUNDED),
                EnumSet.of(ActorType.SYSTEM));
        map.put(new TransitionKey(DealStatus.AWAITING_PAYMENT, DealStatus.CANCELLED),
                EnumSet.of(ActorType.ADVERTISER));
        map.put(new TransitionKey(DealStatus.AWAITING_PAYMENT, DealStatus.EXPIRED),
                EnumSet.of(ActorType.SYSTEM));

        // FUNDED
        map.put(new TransitionKey(DealStatus.FUNDED, DealStatus.CREATIVE_SUBMITTED),
                EnumSet.of(ActorType.CHANNEL_OWNER, ActorType.CHANNEL_ADMIN));
        map.put(new TransitionKey(DealStatus.FUNDED, DealStatus.CANCELLED),
                EnumSet.of(
                        ActorType.ADVERTISER,
                        ActorType.CHANNEL_OWNER,
                        ActorType.CHANNEL_ADMIN));
        map.put(new TransitionKey(DealStatus.FUNDED, DealStatus.EXPIRED),
                EnumSet.of(ActorType.SYSTEM));

        // CREATIVE_SUBMITTED
        map.put(new TransitionKey(DealStatus.CREATIVE_SUBMITTED, DealStatus.CREATIVE_APPROVED),
                EnumSet.of(ActorType.ADVERTISER));
        map.put(new TransitionKey(DealStatus.CREATIVE_SUBMITTED, DealStatus.FUNDED),
                EnumSet.of(ActorType.ADVERTISER));
        map.put(new TransitionKey(DealStatus.CREATIVE_SUBMITTED, DealStatus.DISPUTED),
                EnumSet.of(ActorType.ADVERTISER));

        // CREATIVE_APPROVED
        map.put(new TransitionKey(DealStatus.CREATIVE_APPROVED, DealStatus.SCHEDULED),
                EnumSet.of(ActorType.CHANNEL_OWNER, ActorType.CHANNEL_ADMIN));
        map.put(new TransitionKey(DealStatus.CREATIVE_APPROVED, DealStatus.PUBLISHED),
                EnumSet.of(ActorType.CHANNEL_OWNER, ActorType.CHANNEL_ADMIN));
        map.put(new TransitionKey(DealStatus.CREATIVE_APPROVED, DealStatus.CANCELLED),
                EnumSet.of(
                        ActorType.ADVERTISER,
                        ActorType.CHANNEL_OWNER,
                        ActorType.CHANNEL_ADMIN));
        map.put(new TransitionKey(DealStatus.CREATIVE_APPROVED, DealStatus.EXPIRED),
                EnumSet.of(ActorType.SYSTEM));

        // SCHEDULED
        map.put(new TransitionKey(DealStatus.SCHEDULED, DealStatus.PUBLISHED),
                EnumSet.of(ActorType.SYSTEM));
        map.put(new TransitionKey(DealStatus.SCHEDULED, DealStatus.CANCELLED),
                EnumSet.of(
                        ActorType.ADVERTISER,
                        ActorType.CHANNEL_OWNER,
                        ActorType.CHANNEL_ADMIN));
        map.put(new TransitionKey(DealStatus.SCHEDULED, DealStatus.EXPIRED),
                EnumSet.of(ActorType.SYSTEM));

        // PUBLISHED
        map.put(new TransitionKey(DealStatus.PUBLISHED, DealStatus.DELIVERY_VERIFYING),
                EnumSet.of(ActorType.SYSTEM));

        // DELIVERY_VERIFYING
        map.put(new TransitionKey(DealStatus.DELIVERY_VERIFYING, DealStatus.COMPLETED_RELEASED),
                EnumSet.of(ActorType.SYSTEM));
        map.put(new TransitionKey(DealStatus.DELIVERY_VERIFYING, DealStatus.DISPUTED),
                EnumSet.of(ActorType.SYSTEM));

        // DISPUTED
        map.put(new TransitionKey(DealStatus.DISPUTED, DealStatus.COMPLETED_RELEASED),
                EnumSet.of(ActorType.PLATFORM_OPERATOR));
        map.put(new TransitionKey(DealStatus.DISPUTED, DealStatus.REFUNDED),
                EnumSet.of(ActorType.PLATFORM_OPERATOR));
        map.put(new TransitionKey(DealStatus.DISPUTED, DealStatus.PARTIALLY_REFUNDED),
                EnumSet.of(ActorType.PLATFORM_OPERATOR));

        return Map.copyOf(map);
    }
}

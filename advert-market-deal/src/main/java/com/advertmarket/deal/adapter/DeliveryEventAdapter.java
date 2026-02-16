package com.advertmarket.deal.adapter;

import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.deal.service.DealTransitionService;
import com.advertmarket.delivery.api.event.DeliveryFailedEvent;
import com.advertmarket.delivery.api.event.DeliveryVerifiedEvent;
import com.advertmarket.delivery.api.event.PublicationResultEvent;
import com.advertmarket.delivery.api.port.DeliveryEventPort;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealStatus;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Bridges delivery worker events to deal state transitions.
 */
@Slf4j
@RequiredArgsConstructor
public class DeliveryEventAdapter implements DeliveryEventPort {

    private final DealTransitionService dealTransitionService;
    private final DealRepository dealRepository;

    @Override
    public void onPublicationResult(
            @NonNull EventEnvelope<PublicationResultEvent> envelope) {
        var dealId = Objects.requireNonNull(envelope.dealId(),
                "dealId required for publication result");
        var event = envelope.payload();

        if (!event.success()) {
            log.warn("Publication failed for deal={}, error={}",
                    dealId, event.error());
            return;
        }

        if (event.contentHash() == null || event.publishedAt() == null) {
            log.warn("Publication result missing metadata for deal={}", dealId);
            return;
        }

        dealRepository.setPublicationMetadata(
                dealId,
                event.messageId(),
                event.contentHash(),
                event.publishedAt());

        dealTransitionService.transition(new DealTransitionCommand(
                dealId,
                DealStatus.PUBLISHED,
                null,
                ActorType.SYSTEM,
                "Publication success",
                null,
                null));
    }

    @Override
    public void onDeliveryVerified(
            @NonNull EventEnvelope<DeliveryVerifiedEvent> envelope) {
        var dealId = Objects.requireNonNull(envelope.dealId(),
                "dealId required for delivery verified event");
        dealTransitionService.transition(new DealTransitionCommand(
                dealId,
                DealStatus.COMPLETED_RELEASED,
                null,
                ActorType.SYSTEM,
                "Delivery verified at " + Instant.now(),
                null,
                null));
    }

    @Override
    public void onDeliveryFailed(
            @NonNull EventEnvelope<DeliveryFailedEvent> envelope) {
        var dealId = Objects.requireNonNull(envelope.dealId(),
                "dealId required for delivery failed event");
        dealTransitionService.transition(new DealTransitionCommand(
                dealId,
                DealStatus.DISPUTED,
                null,
                ActorType.SYSTEM,
                "Delivery failed: " + envelope.payload().reason(),
                null,
                null));
    }
}

package com.advertmarket.delivery.api.port;

import com.advertmarket.delivery.api.event.DeliveryFailedEvent;
import com.advertmarket.delivery.api.event.DeliveryVerifiedEvent;
import com.advertmarket.delivery.api.event.PublicationResultEvent;
import com.advertmarket.shared.event.EventEnvelope;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for handling delivery result events from workers.
 */
public interface DeliveryEventPort {

    /**
     * Handles a publication result event.
     *
     * @param envelope the event envelope
     */
    void onPublicationResult(
            @NonNull EventEnvelope<PublicationResultEvent> envelope);

    /**
     * Handles a delivery verified event.
     *
     * @param envelope the event envelope
     */
    void onDeliveryVerified(
            @NonNull EventEnvelope<DeliveryVerifiedEvent> envelope);

    /**
     * Handles a delivery failed event.
     *
     * @param envelope the event envelope
     */
    void onDeliveryFailed(
            @NonNull EventEnvelope<DeliveryFailedEvent> envelope);
}

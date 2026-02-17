package com.advertmarket.app.internal;

import com.advertmarket.delivery.api.event.DeliveryFailedEvent;
import com.advertmarket.delivery.api.event.DeliveryVerifiedEvent;
import com.advertmarket.delivery.api.event.PublicationResultEvent;
import com.advertmarket.delivery.api.port.DeliveryEventPort;
import com.advertmarket.financial.api.event.DepositConfirmedEvent;
import com.advertmarket.financial.api.event.DepositFailedEvent;
import com.advertmarket.financial.api.event.PayoutCompletedEvent;
import com.advertmarket.financial.api.event.ReconciliationResultEvent;
import com.advertmarket.financial.api.event.RefundCompletedEvent;
import com.advertmarket.financial.api.port.FinancialEventPort;
import com.advertmarket.financial.api.port.ReconciliationResultPort;
import com.advertmarket.shared.FenumGroup;
import com.advertmarket.shared.event.DomainEvent;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypeRegistry;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.WorkerCallback;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.fenum.qual.Fenum;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Handles worker callback events received via HTTP.
 *
 * <p>Extracted from the controller to keep {@code ..web..} thin.
 */
@Slf4j
@Component
public class WorkerCallbackHandler {

    private final EventTypeRegistry eventTypeRegistry;
    private final JsonFacade json;
    private final FinancialEventPort financialEventPort;
    private final DeliveryEventPort deliveryEventPort;
    private final ReconciliationResultPort reconciliationResultPort;
    private final MetricsFacade metrics;

    public WorkerCallbackHandler(
            EventTypeRegistry eventTypeRegistry,
            JsonFacade json,
            @Qualifier("financialEventAdapter")
            FinancialEventPort financialEventPort,
            @Qualifier("deliveryEventAdapter")
            DeliveryEventPort deliveryEventPort,
            ReconciliationResultPort reconciliationResultPort,
            MetricsFacade metrics) {
        this.eventTypeRegistry = eventTypeRegistry;
        this.json = json;
        this.financialEventPort = financialEventPort;
        this.deliveryEventPort = deliveryEventPort;
        this.reconciliationResultPort =
                reconciliationResultPort;
        this.metrics = metrics;
    }

    /** Receives a worker callback and dispatches to the appropriate port. */
    @SuppressWarnings("fenum")
    public WorkerCallbackResponse handle(WorkerCallback callback) {
        log.info("Worker callback received: type={}, correlationId={}",
                callback.callbackType(), callback.correlationId());

        metrics.incrementCounter(
                MetricNames.WORKER_CALLBACK_HTTP_RECEIVED,
                "type", callback.callbackType());

        var payloadClass = eventTypeRegistry.resolve(
                callback.callbackType());
        if (payloadClass == null) {
            throw new DomainException(
                    ErrorCodes.UNKNOWN_CALLBACK_TYPE,
                    "Unknown callback type: "
                            + callback.callbackType());
        }

        var payload = (DomainEvent) json.convertValue(
                callback.payload(),
                json.typeFactory().constructType(payloadClass));
        var envelope = buildEnvelope(callback, payload);

        dispatch(callback.callbackType(), envelope);

        return new WorkerCallbackResponse(
                envelope.eventId(),
                envelope.correlationId(),
                "accepted");
    }

    @SuppressWarnings({"unchecked", "fenum"})
    private void dispatch(
            @Fenum(FenumGroup.EVENT_TYPE) String eventType,
            EventEnvelope<?> envelope) {
        switch (eventType) {
            case EventTypes.DEPOSIT_CONFIRMED ->
                    financialEventPort.onDepositConfirmed(
                            (EventEnvelope<DepositConfirmedEvent>) envelope);
            case EventTypes.DEPOSIT_FAILED ->
                    financialEventPort.onDepositFailed(
                            (EventEnvelope<DepositFailedEvent>) envelope);
            case EventTypes.PAYOUT_COMPLETED ->
                    financialEventPort.onPayoutCompleted(
                            (EventEnvelope<PayoutCompletedEvent>) envelope);
            case EventTypes.REFUND_COMPLETED ->
                    financialEventPort.onRefundCompleted(
                            (EventEnvelope<RefundCompletedEvent>) envelope);
            case EventTypes.PUBLICATION_RESULT ->
                    deliveryEventPort.onPublicationResult(
                            (EventEnvelope<PublicationResultEvent>) envelope);
            case EventTypes.DELIVERY_VERIFIED ->
                    deliveryEventPort.onDeliveryVerified(
                            (EventEnvelope<DeliveryVerifiedEvent>) envelope);
            case EventTypes.DELIVERY_FAILED ->
                    deliveryEventPort.onDeliveryFailed(
                            (EventEnvelope<DeliveryFailedEvent>) envelope);
            case EventTypes.RECONCILIATION_RESULT ->
                    reconciliationResultPort.onReconciliationResult(
                            (EventEnvelope<ReconciliationResultEvent>) envelope);
            default -> throw new DomainException(
                    ErrorCodes.UNKNOWN_CALLBACK_TYPE,
                    "Unsupported callback type: " + eventType);
        }
    }

    private <T extends DomainEvent> EventEnvelope<T> buildEnvelope(
            WorkerCallback callback, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                callback.callbackType(),
                callback.dealId(),
                Instant.now(),
                1,
                callback.correlationId(),
                payload);
    }
}

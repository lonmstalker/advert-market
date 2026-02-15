package com.advertmarket.app.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.delivery.api.port.DeliveryEventPort;
import com.advertmarket.financial.api.event.PayoutCompletedEvent;
import com.advertmarket.financial.api.port.FinancialEventPort;
import com.advertmarket.financial.api.port.ReconciliationResultPort;
import com.advertmarket.shared.event.EventTypeRegistry;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.WorkerCallback;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("WorkerCallbackController")
@ExtendWith(MockitoExtension.class)
class WorkerCallbackControllerTest {

    @Mock
    private EventTypeRegistry eventTypeRegistry;
    @Mock
    private JsonFacade json;
    @Mock
    private FinancialEventPort financialEventPort;
    @Mock
    private DeliveryEventPort deliveryEventPort;
    @Mock
    private ReconciliationResultPort reconciliationResultPort;
    @Mock
    private MetricsFacade metrics;

    private WorkerCallbackController controller;

    @BeforeEach
    void setUp() {
        var handler = new WorkerCallbackHandler(
                eventTypeRegistry,
                json,
                financialEventPort,
                deliveryEventPort,
                reconciliationResultPort,
                metrics);
        controller = new WorkerCallbackController(handler);
    }

    @Test
    @DisplayName("Returns 202 Accepted for valid PAYOUT_COMPLETED")
    void validPayoutCompleted_returns202() {
        var objectMapper = new ObjectMapper();
        var payloadNode = objectMapper.valueToTree(
                new PayoutCompletedEvent(
                        "tx1", 1_000_000_000L, 50_000_000L,
                        "addr", 3));
        var callback = new WorkerCallback(
                EventTypes.PAYOUT_COMPLETED, null,
                UUID.randomUUID(), payloadNode);

        doReturn(PayoutCompletedEvent.class)
                .when(eventTypeRegistry)
                .resolve(EventTypes.PAYOUT_COMPLETED);
        when(json.typeFactory())
                .thenReturn(TypeFactory.defaultInstance());
        doReturn(new PayoutCompletedEvent(
                "tx1", 1_000_000_000L, 50_000_000L,
                "addr", 3))
                .when(json).convertValue(eq(payloadNode), any());

        var response = controller.handleCallback(callback);

        assertThat(response.status()).isEqualTo("accepted");
        assertThat(response.correlationId())
                .isEqualTo(callback.correlationId());
        verify(financialEventPort).onPayoutCompleted(any());
        verify(metrics).incrementCounter(
                eq(MetricNames.WORKER_CALLBACK_HTTP_RECEIVED),
                eq("type"), eq(EventTypes.PAYOUT_COMPLETED));
    }

    @Test
    @DisplayName("Unknown callback type throws DomainException")
    void unknownCallbackType_throwsDomainException() {
        var objectMapper = new ObjectMapper();
        var callback = new WorkerCallback(
                "INVALID_TYPE", null,
                UUID.randomUUID(),
                objectMapper.createObjectNode());

        doReturn(null)
                .when(eventTypeRegistry)
                .resolve("INVALID_TYPE");

        assertThatThrownBy(
                () -> controller.handleCallback(callback))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Unknown callback type");
    }
}

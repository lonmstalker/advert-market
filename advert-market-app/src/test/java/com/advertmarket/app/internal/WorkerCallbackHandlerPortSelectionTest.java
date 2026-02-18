package com.advertmarket.app.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypeRegistry;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.WorkerCallback;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.DealId;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@DisplayName("WorkerCallbackHandler port selection")
class WorkerCallbackHandlerPortSelectionTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfig.class)
                    .withBean(WorkerCallbackHandler.class);

    @Test
    @DisplayName("Dispatches DEPOSIT_CONFIRMED to adapter bean when both beans exist")
    void dispatchesDepositConfirmedToAdapterBean() {
        contextRunner.run(context -> {
            var callback = new WorkerCallback(
                    EventTypes.DEPOSIT_CONFIRMED,
                    DealId.generate(),
                    UUID.randomUUID(),
                    Map.of(
                            "txHash", "tx-it-1",
                            "amountNano", 1_500_000_000L,
                            "expectedAmountNano", 1_500_000_000L,
                            "confirmations", 3,
                            "fromAddress", "UQ_from",
                            "depositAddress", "UQ_deposit"));
            var handler = context.getBean(WorkerCallbackHandler.class);
            var adapterFinancialPort =
                    context.getBean(AdapterFinancialPort.class);

            var response = handler.handle(callback);

            assertThat(response.status()).isEqualTo("accepted");
            assertThat(adapterFinancialPort.depositConfirmedCalls())
                    .isEqualTo(1);
        });
    }

    @Configuration
    static class TestConfig {

        @Bean
        EventTypeRegistry eventTypeRegistry() {
            var registry = new EventTypeRegistry();
            registry.register(
                    EventTypes.DEPOSIT_CONFIRMED,
                    DepositConfirmedEvent.class);
            return registry;
        }

        @Bean
        JsonFacade jsonFacade() {
            return new JsonFacade(new ObjectMapper());
        }

        @Bean
        MetricsFacade metricsFacade() {
            return mock(MetricsFacade.class);
        }

        @Bean(name = "financialEventPort")
        FinancialEventPort stubFinancialEventPort() {
            return new FinancialEventPort() {
                @Override
                public void onDepositConfirmed(
                        EventEnvelope<DepositConfirmedEvent> envelope) {
                }

                @Override
                public void onDepositFailed(
                        EventEnvelope<DepositFailedEvent> envelope) {
                }

                @Override
                public void onPayoutCompleted(
                        EventEnvelope<PayoutCompletedEvent> envelope) {
                }

                @Override
                public void onRefundCompleted(
                        EventEnvelope<RefundCompletedEvent> envelope) {
                }
            };
        }

        @Bean(name = "financialEventAdapter")
        AdapterFinancialPort financialEventAdapter() {
            return new AdapterFinancialPort();
        }

        @Bean(name = "deliveryEventPort")
        DeliveryEventPort stubDeliveryEventPort() {
            return new DeliveryEventPort() {
                @Override
                public void onPublicationResult(
                        EventEnvelope<PublicationResultEvent> envelope) {
                }

                @Override
                public void onDeliveryVerified(
                        EventEnvelope<DeliveryVerifiedEvent> envelope) {
                }

                @Override
                public void onDeliveryFailed(
                        EventEnvelope<DeliveryFailedEvent> envelope) {
                }
            };
        }

        @Bean(name = "deliveryEventAdapter")
        DeliveryEventPort deliveryEventAdapter() {
            return new DeliveryEventPort() {
                @Override
                public void onPublicationResult(
                        EventEnvelope<PublicationResultEvent> envelope) {
                }

                @Override
                public void onDeliveryVerified(
                        EventEnvelope<DeliveryVerifiedEvent> envelope) {
                }

                @Override
                public void onDeliveryFailed(
                        EventEnvelope<DeliveryFailedEvent> envelope) {
                }
            };
        }

        @Bean(name = "reconciliationResultPort")
        ReconciliationResultPort reconciliationResultPort() {
            return new ReconciliationResultPort() {
                @Override
                public void onReconciliationResult(
                        EventEnvelope<ReconciliationResultEvent> envelope) {
                }
            };
        }
    }

    static class AdapterFinancialPort implements FinancialEventPort {

        private final AtomicInteger depositConfirmedCalls =
                new AtomicInteger(0);

        @Override
        public void onDepositConfirmed(
                EventEnvelope<DepositConfirmedEvent> envelope) {
            depositConfirmedCalls.incrementAndGet();
        }

        @Override
        public void onDepositFailed(
                EventEnvelope<DepositFailedEvent> envelope) {
        }

        @Override
        public void onPayoutCompleted(
                EventEnvelope<PayoutCompletedEvent> envelope) {
        }

        @Override
        public void onRefundCompleted(
                EventEnvelope<RefundCompletedEvent> envelope) {
        }

        int depositConfirmedCalls() {
            return depositConfirmedCalls.get();
        }
    }
}

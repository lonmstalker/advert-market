package com.advertmarket.app.config;

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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * No-op fallback implementations for worker event ports.
 *
 * <p>Used while delivery/financial modules are scaffold-only.
 * Automatically replaced once real beans appear.
 */
@Configuration
public class WorkerEventPortStubConfig {

    @Bean
    @ConditionalOnMissingBean
    FinancialEventPort financialEventPort() {
        return new FinancialEventPort() {
            @Override
            public void onDepositConfirmed(
                    @NonNull EventEnvelope<DepositConfirmedEvent> envelope) {
            }

            @Override
            public void onDepositFailed(
                    @NonNull EventEnvelope<DepositFailedEvent> envelope) {
            }

            @Override
            public void onPayoutCompleted(
                    @NonNull EventEnvelope<PayoutCompletedEvent> envelope) {
            }

            @Override
            public void onRefundCompleted(
                    @NonNull EventEnvelope<RefundCompletedEvent> envelope) {
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    DeliveryEventPort deliveryEventPort() {
        return new DeliveryEventPort() {
            @Override
            public void onPublicationResult(
                    @NonNull EventEnvelope<PublicationResultEvent> envelope) {
            }

            @Override
            public void onDeliveryVerified(
                    @NonNull EventEnvelope<DeliveryVerifiedEvent> envelope) {
            }

            @Override
            public void onDeliveryFailed(
                    @NonNull EventEnvelope<DeliveryFailedEvent> envelope) {
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    ReconciliationResultPort reconciliationResultPort() {
        return new ReconciliationResultPort() {
            @Override
            public void onReconciliationResult(
                    @NonNull EventEnvelope<ReconciliationResultEvent> envelope) {
            }
        };
    }
}

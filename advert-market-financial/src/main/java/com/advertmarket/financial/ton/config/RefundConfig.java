package com.advertmarket.financial.ton.config;

import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.config.NetworkFeeProperties;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.financial.ton.service.RefundExecutorWorker;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.outbox.OutboxRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires refund executor beans.
 */
@Configuration
@EnableConfigurationProperties(NetworkFeeProperties.class)
public class RefundConfig {

    @Bean
    RefundExecutorWorker refundExecutorWorker(
            TonWalletPort tonWalletPort,
            LedgerPort ledgerPort,
            OutboxRepository outboxRepository,
            DistributedLockPort lockPort,
            JsonFacade jsonFacade,
            MetricsFacade metrics,
            RefundExecutorDependencies dependencies) {
        return new RefundExecutorWorker(
                tonWalletPort, ledgerPort,
                outboxRepository, lockPort, jsonFacade, metrics,
                dependencies.txRepository(),
                dependencies.networkFeeProperties());
    }

    @Bean
    RefundExecutorDependencies refundExecutorDependencies(
            JooqTonTransactionRepository txRepository,
            NetworkFeeProperties networkFeeProperties) {
        return new RefundExecutorDependencies(
                txRepository,
                networkFeeProperties);
    }

    private record RefundExecutorDependencies(
            JooqTonTransactionRepository txRepository,
            NetworkFeeProperties networkFeeProperties) {}
}

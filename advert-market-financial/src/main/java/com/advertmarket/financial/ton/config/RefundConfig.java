package com.advertmarket.financial.ton.config;

import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.ton.service.RefundExecutorWorker;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.outbox.OutboxRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires refund executor beans.
 */
@Configuration
public class RefundConfig {

    @Bean
    RefundExecutorWorker refundExecutorWorker(
            TonWalletPort tonWalletPort,
            LedgerPort ledgerPort,
            OutboxRepository outboxRepository,
            DistributedLockPort lockPort,
            JsonFacade jsonFacade,
            MetricsFacade metrics) {
        return new RefundExecutorWorker(
                tonWalletPort, ledgerPort,
                outboxRepository, lockPort, jsonFacade, metrics);
    }
}

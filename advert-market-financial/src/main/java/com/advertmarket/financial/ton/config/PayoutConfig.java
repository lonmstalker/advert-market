package com.advertmarket.financial.ton.config;

import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.config.NetworkFeeProperties;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.financial.ton.service.PayoutExecutorWorker;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.outbox.OutboxRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires payout executor beans.
 */
@Configuration
@EnableConfigurationProperties(NetworkFeeProperties.class)
public class PayoutConfig {

    @Bean
    @SuppressWarnings("checkstyle:ParameterNumber")
    PayoutExecutorWorker payoutExecutorWorker(
            TonWalletPort tonWalletPort,
            LedgerPort ledgerPort,
            UserRepository userRepository,
            OutboxRepository outboxRepository,
            DistributedLockPort lockPort,
            JsonFacade jsonFacade,
            MetricsFacade metrics,
            JooqTonTransactionRepository txRepository,
            NetworkFeeProperties networkFeeProperties) {
        return new PayoutExecutorWorker(
                tonWalletPort, ledgerPort, userRepository,
                outboxRepository, lockPort, jsonFacade, metrics,
                txRepository, networkFeeProperties);
    }
}

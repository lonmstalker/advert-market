package com.advertmarket.financial.escrow.config;

import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.escrow.service.EscrowService;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.shared.metric.MetricsFacade;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires escrow domain beans.
 */
@Configuration
public class EscrowConfig {

    @Bean
    EscrowService escrowService(
            TonWalletPort tonWalletPort,
            LedgerPort ledgerPort,
            JooqTonTransactionRepository txRepository,
            MetricsFacade metrics) {
        return new EscrowService(tonWalletPort, ledgerPort,
                txRepository, metrics);
    }
}

package com.advertmarket.financial.wallet.config;

import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.config.WalletProperties;
import com.advertmarket.financial.wallet.service.WalletService;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.metric.MetricsFacade;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires wallet domain beans.
 */
@Configuration
@EnableConfigurationProperties(WalletProperties.class)
public class WalletConfig {

    @Bean
    WalletService walletService(
            LedgerPort ledgerPort,
            UserRepository userRepository,
            MetricsFacade metrics,
            WalletProperties walletProperties) {
        return new WalletService(
                ledgerPort, userRepository, metrics,
                walletProperties.minWithdrawalNano());
    }
}

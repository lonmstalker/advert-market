package com.advertmarket.financial.wallet.config;

import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.wallet.service.WalletService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires wallet domain beans.
 */
@Configuration
public class WalletConfig {

    @Bean
    WalletService walletService(LedgerPort ledgerPort) {
        return new WalletService(ledgerPort);
    }
}

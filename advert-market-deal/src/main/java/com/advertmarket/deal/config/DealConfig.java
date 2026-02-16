package com.advertmarket.deal.config;

import com.advertmarket.deal.adapter.FinancialEventAdapter;
import com.advertmarket.deal.api.port.DealEventRepository;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.deal.service.DealTransitionService;
import com.advertmarket.financial.api.port.EscrowPort;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.outbox.OutboxRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires deal domain beans that are not Spring-managed components.
 */
@Configuration
@EnableConfigurationProperties(DealTimeoutProperties.class)
public class DealConfig {

    @Bean
    DealTransitionService dealTransitionService(
            DealRepository dealRepository,
            DealEventRepository dealEventRepository,
            OutboxRepository outboxRepository,
            JsonFacade jsonFacade) {
        return new DealTransitionService(
                dealRepository, dealEventRepository,
                outboxRepository, jsonFacade);
    }

    @Bean
    FinancialEventAdapter financialEventAdapter(
            DealTransitionService dealTransitionService,
            EscrowPort escrowPort) {
        return new FinancialEventAdapter(dealTransitionService, escrowPort);
    }
}

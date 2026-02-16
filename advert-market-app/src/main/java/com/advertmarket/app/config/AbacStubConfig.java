package com.advertmarket.app.config;

import com.advertmarket.deal.api.port.DealAuthorizationPort;
import com.advertmarket.marketplace.api.model.ChannelRight;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.shared.model.DealId;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stub implementations for ABAC authorization ports.
 *
 * <p>Deny-all by default. Replaced by real {@code @Component}
 * implementations when the respective modules are fully implemented.
 */
@Configuration
public class AbacStubConfig {

    @Bean
    @ConditionalOnMissingBean
    DealAuthorizationPort dealAuthorizationPort() {
        return new DealAuthorizationPort() {
            @Override
            public boolean isParticipant(@NonNull DealId dealId) {
                return false;
            }

            @Override
            public boolean isAdvertiser(@NonNull DealId dealId) {
                return false;
            }

            @Override
            public boolean isOwner(@NonNull DealId dealId) {
                return false;
            }

            @Override
            public long getChannelId(@NonNull DealId dealId) {
                return 0;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    ChannelAuthorizationPort channelAuthorizationPort() {
        return new ChannelAuthorizationPort() {
            @Override
            public boolean isOwner(long channelId) {
                return false;
            }

            @Override
            public boolean hasRight(long channelId,
                    @NonNull ChannelRight right) {
                return false;
            }
        };
    }
}

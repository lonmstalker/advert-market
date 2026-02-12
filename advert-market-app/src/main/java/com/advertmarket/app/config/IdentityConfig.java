package com.advertmarket.app.config;

import com.advertmarket.communication.bot.internal.config.TelegramBotProperties;
import com.advertmarket.identity.api.port.TokenBlacklistPort;
import com.advertmarket.identity.config.AuthProperties;
import com.advertmarket.identity.config.RateLimiterProperties;
import com.advertmarket.identity.security.JwtAuthenticationFilter;
import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.identity.service.TelegramInitDataValidator;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.UserBlockCheckPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires identity beans that need cross-module dependencies.
 */
@Configuration
@EnableConfigurationProperties({AuthProperties.class, RateLimiterProperties.class})
public class IdentityConfig {

    /** Creates the JWT token provider. */
    @Bean
    public JwtTokenProvider jwtTokenProvider(
            AuthProperties authProperties) {
        return new JwtTokenProvider(authProperties);
    }

    /** Creates the Telegram initData validator. */
    @Bean
    public TelegramInitDataValidator telegramInitDataValidator(
            TelegramBotProperties telegramBotProperties,
            AuthProperties authProperties,
            JsonFacade jsonFacade) {
        return new TelegramInitDataValidator(
                telegramBotProperties.botToken(),
                authProperties,
                jsonFacade);
    }

    /** Creates the JWT authentication filter. */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            TokenBlacklistPort tokenBlacklistPort,
            UserBlockCheckPort userBlockCheckPort) {
        return new JwtAuthenticationFilter(
                jwtTokenProvider,
                tokenBlacklistPort,
                userBlockCheckPort);
    }
}

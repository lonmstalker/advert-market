package com.advertmarket.app.config;

import com.advertmarket.app.error.SecurityExceptionHandler;
import com.advertmarket.app.filter.InternalApiKeyFilter;
import com.advertmarket.identity.security.JwtAuthenticationFilter;
import com.advertmarket.shared.metric.MetricsFacade;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Spring Security configuration.
 */
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
@EnableConfigurationProperties({CorsProperties.class, InternalApiProperties.class})
public class SecurityConfig {

    private static final long HSTS_MAX_AGE_SECONDS =
            Duration.ofDays(365).toSeconds();

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsProperties corsProperties;
    private final SecurityExceptionHandler securityExceptionHandler;
    private final InternalApiProperties internalApiProperties;
    private final MetricsFacade metricsFacade;

    /** Internal API security chain — API key auth, no JWT. */
    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurityFilterChain(
            HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/internal/v1/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .addFilterBefore(
                        new InternalApiKeyFilter(
                                internalApiProperties, metricsFacade),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** Public API security chain — JWT auth. */
    @Bean
    @Order(2)
    public SecurityFilterChain publicSecurityFilterChain(
            HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new CorsConfiguration();
                    config.setAllowedOrigins(
                            corsProperties.allowedOrigins());
                    config.setAllowedMethods(List.of(
                            "GET", "POST", "PUT", "DELETE",
                            "OPTIONS"));
                    config.setAllowedHeaders(List.of(
                            "Authorization", "Content-Type",
                            "X-Telegram-Init-Data",
                            "X-Correlation-Id"));
                    config.setAllowCredentials(false);
                    config.setMaxAge(Duration.ofHours(1).toSeconds());
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentTypeOptions(
                                Customizer.withDefaults())
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/bot/webhook",
                                "/actuator/health/**",
                                "/v3/api-docs",
                                "/v3/api-docs.yaml",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers(
                                "/actuator/prometheus",
                                "/actuator/metrics",
                                "/actuator/info")
                        .authenticated()
                        .requestMatchers("/api/v1/**")
                        .authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                securityExceptionHandler)
                        .accessDeniedHandler(
                                securityExceptionHandler))
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

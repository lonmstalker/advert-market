package com.advertmarket.integration.marketplace.config;

import com.advertmarket.communication.bot.internal.block.RedisUserBlockService;
import com.advertmarket.communication.bot.internal.block.UserBlockProperties;
import com.advertmarket.identity.adapter.JooqUserRepository;
import com.advertmarket.identity.adapter.RedisLoginRateLimiter;
import com.advertmarket.identity.adapter.RedisTokenBlacklist;
import com.advertmarket.identity.api.port.LoginRateLimiterPort;
import com.advertmarket.identity.api.port.TokenBlacklistPort;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.identity.config.AuthProperties;
import com.advertmarket.identity.config.RateLimiterProperties;
import com.advertmarket.identity.security.JwtAuthenticationFilter;
import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.shared.error.ErrorCode;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.UserBlockCheckPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Shared Spring configuration for marketplace HTTP integration tests.
 *
 * <p>Provides common infrastructure beans: DSLContext, JWT auth, security filter,
 * exception handling, metrics, localization.
 */
@Configuration
@EnableMethodSecurity
public class MarketplaceTestConfig {

    static final String JWT_SIGN_KEY =
            "integration-test-key-min-32-bytes!!!";

    @Bean
    DSLContext dslContext(DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }

    @Bean
    AuthProperties authProperties() {
        return new AuthProperties(
                new AuthProperties.Jwt(JWT_SIGN_KEY, 3600),
                300);
    }

    @Bean
    RateLimiterProperties rateLimiterProperties() {
        return new RateLimiterProperties(10, 60);
    }

    @Bean
    JwtTokenProvider jwtTokenProvider(AuthProperties props) {
        return new JwtTokenProvider(props);
    }

    @Bean
    ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Bean
    JsonFacade jsonFacade(ObjectMapper om) {
        return new JsonFacade(om);
    }

    @Bean
    MetricsFacade metricsFacade() {
        return new MetricsFacade(new SimpleMeterRegistry());
    }

    @Bean
    LocalizationService localizationService() {
        return new LocalizationService(new StaticMessageSource());
    }

    @Bean
    UserRepository userRepository(DSLContext dsl) {
        return new JooqUserRepository(dsl);
    }

    @Bean
    TokenBlacklistPort tokenBlacklistPort(StringRedisTemplate tpl) {
        return new RedisTokenBlacklist(tpl);
    }

    @Bean
    LoginRateLimiterPort loginRateLimiterPort(
            StringRedisTemplate tpl,
            RateLimiterProperties props,
            MetricsFacade mf) {
        return new RedisLoginRateLimiter(tpl, props, mf);
    }

    @Bean
    UserBlockCheckPort userBlockCheckPort(StringRedisTemplate tpl) {
        return new RedisUserBlockService(
                tpl, new UserBlockProperties("tg:block:"));
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenProvider jwt,
            TokenBlacklistPort bl,
            UserBlockCheckPort ub) {
        return new JwtAuthenticationFilter(jwt, bl, ub);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/channels",
                                "/api/v1/channels/*",
                                "/api/v1/channels/*/pricing",
                                "/api/v1/categories",
                                "/api/v1/post-types")
                        .permitAll()
                        .requestMatchers("/api/v1/**")
                        .authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(jwtFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    TestExceptionHandler testExceptionHandler() {
        return new TestExceptionHandler();
    }

    @RestControllerAdvice
    static class TestExceptionHandler
            extends ResponseEntityExceptionHandler {

        @ExceptionHandler(DomainException.class)
        ProblemDetail handleDomain(DomainException ex) {
            var code = ErrorCode.resolve(ex.getErrorCode());
            int status = code != null
                    ? code.httpStatus() : 500;
            var pd = ProblemDetail.forStatus(status);
            if (code != null) {
                pd.setType(URI.create(code.typeUri()));
                pd.setTitle(code.name());
            }
            pd.setDetail(ex.getMessage());
            addProps(pd, ex.getErrorCode());
            return pd;
        }

        @ExceptionHandler(EntityNotFoundException.class)
        ProblemDetail handleNotFound(EntityNotFoundException ex) {
            return handleDomain(ex);
        }

        @Override
        protected ResponseEntity<Object> handleMethodArgumentNotValid(
                MethodArgumentNotValidException ex,
                HttpHeaders headers,
                HttpStatusCode status,
                WebRequest request) {
            var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            pd.setType(URI.create(
                    "urn:problem-type:validation-failed"));
            pd.setTitle("Validation failed");
            pd.setDetail(ex.getBindingResult()
                    .getFieldErrors().stream()
                    .map(e -> e.getField() + ": "
                            + e.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed"));
            addProps(pd, "VALIDATION_FAILED");
            return ResponseEntity.badRequest().body(pd);
        }

        @Override
        protected ResponseEntity<Object> handleHttpMessageNotReadable(
                HttpMessageNotReadableException ex,
                HttpHeaders headers,
                HttpStatusCode status,
                WebRequest request) {
            var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            pd.setType(URI.create(
                    "urn:problem-type:validation-failed"));
            pd.setTitle("Malformed body");
            pd.setDetail("Missing or invalid JSON");
            addProps(pd, "VALIDATION_FAILED");
            return ResponseEntity.badRequest().body(pd);
        }

        private void addProps(ProblemDetail pd, String ec) {
            pd.setProperty("error_code", ec);
            pd.setProperty("timestamp", Instant.now().toString());
            pd.setProperty("correlation_id",
                    UUID.randomUUID().toString());
        }
    }
}

package com.advertmarket.integration.identity;

import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.identity.adapter.JooqUserRepository;
import com.advertmarket.identity.adapter.RedisLoginRateLimiter;
import com.advertmarket.identity.adapter.RedisTokenBlacklist;
import com.advertmarket.identity.api.dto.OnboardingRequest;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.LoginRateLimiterPort;
import com.advertmarket.identity.api.port.TokenBlacklistPort;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.identity.config.AuthProperties;
import com.advertmarket.identity.config.RateLimiterProperties;
import com.advertmarket.identity.security.JwtAuthenticationFilter;
import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.identity.service.AuthServiceImpl;
import com.advertmarket.identity.service.TelegramInitDataValidator;
import com.advertmarket.identity.service.UserServiceImpl;
import com.advertmarket.identity.web.AuthController;
import com.advertmarket.identity.web.ProfileController;
import com.advertmarket.shared.error.ErrorCode;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.model.UserBlockCheckPort;
import com.advertmarket.communication.bot.internal.block.RedisUserBlockService;
import com.advertmarket.communication.bot.internal.block.UserBlockProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * HTTP-level integration tests for Auth and Profile endpoints.
 *
 * <p>Uses a minimal Spring context with real PostgreSQL and Redis
 * via Testcontainers. Validates request/response contracts
 * including ProblemDetail format.
 */
@SpringBootTest(
        classes = AuthHttpIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@DisplayName("Auth HTTP — end-to-end integration")
class AuthHttpIntegrationTest {

    private static final long TEST_USER_ID = 42L;
    private static final String JWT_SECRET =
            "test-secret-key-at-least-32-bytes-long!!";

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:8.4-alpine")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                postgres::getJdbcUrl);
        registry.add("spring.datasource.username",
                postgres::getUsername);
        registry.add("spring.datasource.password",
                postgres::getPassword);
        registry.add("spring.data.redis.host",
                redis::getHost);
        registry.add("spring.data.redis.port",
                () -> redis.getMappedPort(6379));
    }

    @BeforeAll
    static void initDatabase() throws Exception {
        try (var dslCtx = DSL.using(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            var conn = dslCtx.configuration()
                    .connectionProvider().acquire();
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(
                            new JdbcConnection(conn));
            try (var liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.yaml",
                    new ClassLoaderResourceAccessor(),
                    database)) {
                liquibase.update("");
            }
        }
    }

    @LocalServerPort
    private int port;

    private WebTestClient webClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DSLContext dsl;

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        dsl.deleteFrom(USERS).execute();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with empty body returns 400 + ProblemDetail")
    void loginWithEmptyBody_returns400() {
        webClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.type").isNotEmpty()
                .jsonPath("$.title").isNotEmpty()
                .jsonPath("$.error_code").isNotEmpty()
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/profile without Authorization header returns 403")
    void profileWithoutAuth_returnsForbidden() {
        webClient.get().uri("/api/v1/profile")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("GET /api/v1/profile with valid JWT returns 200 + UserProfile")
    void profileWithValidJwt_returns200() {
        userRepository.upsert(new TelegramUserData(
                TEST_USER_ID, "John", "Doe", "johndoe", "en"));
        String token = jwtTokenProvider.generateToken(
                new UserId(TEST_USER_ID), false);

        UserProfile profile = webClient.get()
                .uri("/api/v1/profile")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserProfile.class)
                .returnResult()
                .getResponseBody();

        assertThat(profile).isNotNull();
        assertThat(profile.id()).isEqualTo(TEST_USER_ID);
        assertThat(profile.username()).isEqualTo("johndoe");
        assertThat(profile.displayName())
                .isEqualTo("John Doe");
        assertThat(profile.languageCode()).isEqualTo("en");
        assertThat(profile.onboardingCompleted()).isFalse();
    }

    @Test
    @DisplayName("PUT /api/v1/profile/onboarding with valid JWT completes onboarding")
    void onboardingWithValidJwt_returns200() {
        userRepository.upsert(new TelegramUserData(
                TEST_USER_ID, "John", "Doe", "johndoe", "en"));
        String token = jwtTokenProvider.generateToken(
                new UserId(TEST_USER_ID), false);

        UserProfile profile = webClient.put()
                .uri("/api/v1/profile/onboarding")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OnboardingRequest(
                        List.of("advertiser")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserProfile.class)
                .returnResult()
                .getResponseBody();

        assertThat(profile).isNotNull();
        assertThat(profile.onboardingCompleted()).isTrue();
        assertThat(profile.interests())
                .containsExactly("advertiser");
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout then GET /api/v1/profile returns 403")
    void logoutThenProfile_returnsForbidden() {
        userRepository.upsert(new TelegramUserData(
                TEST_USER_ID, "John", "Doe", "johndoe", "en"));
        String token = jwtTokenProvider.generateToken(
                new UserId(TEST_USER_ID), false);

        webClient.post().uri("/api/v1/auth/logout")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(
                        HttpStatus.NO_CONTENT);

        webClient.get().uri("/api/v1/profile")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("ProblemDetail contains required fields: type, title, status, error_code, timestamp, correlation_id")
    void problemDetail_hasRequiredFields() {
        Map<String, Object> body = webClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(
                        new ParameterizedTypeReference
                                <Map<String, Object>>() {
                        })
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body).containsKey("type");
        assertThat(body).containsKey("title");
        assertThat(body).containsKey("status");
        assertThat(body).containsKey("error_code");
        assertThat(body).containsKey("timestamp");
        assertThat(body).containsKey("correlation_id");
        assertThat(body.get("error_code"))
                .isEqualTo("VALIDATION_FAILED");
    }

    /**
     * Minimal Spring Boot config for auth HTTP tests.
     */
    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {

        @Bean
        DSLContext dslContext(DataSource dataSource) {
            return DSL.using(dataSource, SQLDialect.POSTGRES);
        }

        @Bean
        AuthProperties authProperties() {
            return new AuthProperties(
                    new AuthProperties.Jwt(JWT_SECRET, 3600),
                    300);
        }

        @Bean
        RateLimiterProperties rateLimiterProperties() {
            return new RateLimiterProperties(10, 60);
        }

        @Bean
        JwtTokenProvider jwtTokenProvider(
                AuthProperties authProperties) {
            return new JwtTokenProvider(authProperties);
        }

        @Bean
        ObjectMapper objectMapper() {
            var mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper;
        }

        @Bean
        JsonFacade jsonFacade(ObjectMapper objectMapper) {
            return new JsonFacade(objectMapper);
        }

        @Bean
        MetricsFacade metricsFacade() {
            return new MetricsFacade(new SimpleMeterRegistry());
        }

        @Bean
        LocalizationService localizationService() {
            return new LocalizationService(
                    new StaticMessageSource());
        }

        @Bean
        UserRepository userRepository(DSLContext dsl) {
            return new JooqUserRepository(dsl);
        }

        @Bean
        TokenBlacklistPort tokenBlacklistPort(
                StringRedisTemplate redisTemplate) {
            return new RedisTokenBlacklist(redisTemplate);
        }

        @Bean
        LoginRateLimiterPort loginRateLimiterPort(
                StringRedisTemplate redisTemplate,
                RateLimiterProperties properties,
                MetricsFacade metricsFacade) {
            return new RedisLoginRateLimiter(
                    redisTemplate, properties, metricsFacade);
        }

        @Bean
        UserBlockCheckPort userBlockCheckPort(
                StringRedisTemplate redisTemplate) {
            return new RedisUserBlockService(
                    redisTemplate,
                    new UserBlockProperties("tg:block:"));
        }

        @Bean
        TelegramInitDataValidator telegramInitDataValidator(
                AuthProperties authProperties,
                JsonFacade jsonFacade) {
            return new TelegramInitDataValidator(
                    "123456:TEST-TOKEN",
                    authProperties, jsonFacade);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                JwtTokenProvider jwtTokenProvider,
                TokenBlacklistPort tokenBlacklistPort,
                UserBlockCheckPort userBlockCheckPort) {
            return new JwtAuthenticationFilter(
                    jwtTokenProvider,
                    tokenBlacklistPort,
                    userBlockCheckPort);
        }

        @Bean
        SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                JwtAuthenticationFilter jwtFilter)
                throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(
                                    SessionCreationPolicy
                                            .STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(
                                    "/api/v1/auth/login")
                            .permitAll()
                            .requestMatchers("/api/v1/**")
                            .authenticated()
                            .anyRequest().denyAll())
                    .addFilterBefore(jwtFilter,
                            UsernamePasswordAuthenticationFilter
                                    .class)
                    .build();
        }

        @Bean
        AuthServiceImpl authService(
                TelegramInitDataValidator validator,
                UserRepository userRepository,
                JwtTokenProvider jwtTokenProvider,
                TokenBlacklistPort tokenBlacklistPort,
                MetricsFacade metricsFacade) {
            return new AuthServiceImpl(
                    validator, userRepository,
                    jwtTokenProvider, tokenBlacklistPort,
                    metricsFacade);
        }

        @Bean
        UserServiceImpl userService(
                UserRepository userRepository,
                MetricsFacade metricsFacade) {
            return new UserServiceImpl(
                    userRepository, metricsFacade);
        }

        @Bean
        AuthController authController(
                AuthServiceImpl authService,
                LoginRateLimiterPort loginRateLimiterPort) {
            return new AuthController(
                    authService, loginRateLimiterPort);
        }

        @Bean
        ProfileController profileController(
                UserServiceImpl userService,
                AuthServiceImpl authService) {
            return new ProfileController(
                    userService, authService);
        }

        @Bean
        TestExceptionHandler testExceptionHandler() {
            return new TestExceptionHandler();
        }
    }

    /**
     * Test error handler producing ProblemDetail responses.
     */
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
            addCommonProps(pd, ex.getErrorCode());
            return pd;
        }

        @ExceptionHandler(EntityNotFoundException.class)
        ProblemDetail handleNotFound(
                EntityNotFoundException ex) {
            return handleDomain(ex);
        }

        @Override
        protected ResponseEntity<Object>
                handleMethodArgumentNotValid(
                MethodArgumentNotValidException ex,
                HttpHeaders headers,
                HttpStatusCode status,
                WebRequest request) {
            var pd = ProblemDetail.forStatus(
                    HttpStatus.BAD_REQUEST);
            pd.setType(URI.create(
                    "urn:problem-type:validation-failed"));
            pd.setTitle("Validation failed");
            pd.setDetail(ex.getBindingResult()
                    .getFieldErrors().stream()
                    .map(e -> e.getField() + ": "
                            + e.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed"));
            addCommonProps(pd, "VALIDATION_FAILED");
            return ResponseEntity.badRequest().body(pd);
        }

        @Override
        protected ResponseEntity<Object>
                handleHttpMessageNotReadable(
                HttpMessageNotReadableException ex,
                HttpHeaders headers,
                HttpStatusCode status,
                WebRequest request) {
            var pd = ProblemDetail.forStatus(
                    HttpStatus.BAD_REQUEST);
            pd.setType(URI.create(
                    "urn:problem-type:validation-failed"));
            pd.setTitle("Malformed request body");
            pd.setDetail("Request body is missing or"
                    + " contains invalid JSON");
            addCommonProps(pd, "VALIDATION_FAILED");
            return ResponseEntity.badRequest().body(pd);
        }

        private void addCommonProps(
                ProblemDetail pd, String errorCode) {
            pd.setProperty("error_code", errorCode);
            pd.setProperty("timestamp",
                    Instant.now().toString());
            pd.setProperty("correlation_id",
                    UUID.randomUUID().toString());
        }
    }
}

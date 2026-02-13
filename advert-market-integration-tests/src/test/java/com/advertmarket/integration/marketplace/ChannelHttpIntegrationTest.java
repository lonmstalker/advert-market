package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.api.channel.ChatInfo;
import com.advertmarket.communication.api.channel.ChatMemberInfo;
import com.advertmarket.communication.api.channel.ChatMemberStatus;
import com.advertmarket.communication.api.channel.TelegramChannelPort;
import com.advertmarket.identity.adapter.JooqUserRepository;
import com.advertmarket.identity.adapter.RedisLoginRateLimiter;
import com.advertmarket.identity.adapter.RedisTokenBlacklist;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.port.LoginRateLimiterPort;
import com.advertmarket.identity.api.port.TokenBlacklistPort;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.identity.config.AuthProperties;
import com.advertmarket.identity.config.RateLimiterProperties;
import com.advertmarket.identity.security.JwtAuthenticationFilter;
import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.marketplace.api.dto.ChannelRegistrationRequest;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelVerifyRequest;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.api.port.CategoryRepository;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.channel.config.ChannelBotProperties;
import com.advertmarket.marketplace.channel.mapper.ChannelRecordMapper;
import com.advertmarket.marketplace.channel.repository.JooqCategoryRepository;
import com.advertmarket.marketplace.channel.repository.JooqChannelRepository;
import com.advertmarket.marketplace.channel.service.ChannelRegistrationService;
import com.advertmarket.marketplace.channel.service.ChannelService;
import com.advertmarket.marketplace.channel.service.ChannelVerificationService;
import com.advertmarket.marketplace.channel.web.ChannelController;
import com.advertmarket.marketplace.pricing.mapper.PricingRuleRecordMapper;
import com.advertmarket.marketplace.pricing.repository.JooqPricingRuleRepository;
import com.advertmarket.shared.error.ErrorCode;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.UserBlockCheckPort;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.communication.bot.internal.block.RedisUserBlockService;
import com.advertmarket.communication.bot.internal.block.UserBlockProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.time.Instant;
import java.util.List;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * HTTP-level integration tests for Channel endpoints.
 */
@SpringBootTest(
        classes = ChannelHttpIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@DisplayName("Channel HTTP — end-to-end integration")
class ChannelHttpIntegrationTest {

    private static final long TEST_USER_ID = 1L;
    private static final long BOT_USER_ID = 2L;
    private static final long CHAN_TG_ID = -100L;
    private static final String CHAN_TITLE = "Chan A";
    private static final String CHAN_UNAME = "chan_a";
    private static final String JWT_SIGN_KEY =
            "integration-test-key-min-32-bytes!!!";

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName
                    .parse("paradedb/paradedb:latest")
                    .asCompatibleSubstituteFor("postgres"));

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
    private DSLContext dsl;

    @Autowired
    private TelegramChannelPort telegramChannelPort;

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        reset(telegramChannelPort);
        dsl.deleteFrom(CHANNEL_CATEGORIES).execute();
        dsl.deleteFrom(CHANNEL_MEMBERSHIPS).execute();
        dsl.deleteFrom(CHANNELS).execute();
        dsl.deleteFrom(USERS).execute();
        upsertUser(TEST_USER_ID);
        configureMockHappyPath();
    }

    @Test
    @DisplayName("POST /api/v1/channels/verify returns 200 on success")
    void verifySuccess_returns200() {
        String token = jwt(TEST_USER_ID);

        ChannelVerifyResponse body = webClient.post()
                .uri("/api/v1/channels/verify")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelVerifyRequest(CHAN_UNAME))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChannelVerifyResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.channelId()).isEqualTo(CHAN_TG_ID);
        assertThat(body.title()).isEqualTo(CHAN_TITLE);
        assertThat(body.botStatus().isAdmin()).isTrue();
        assertThat(body.userStatus().isMember()).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/channels/verify with blank username returns 400")
    void verifyBlankUsername_returns400() {
        String token = jwt(TEST_USER_ID);

        webClient.post()
                .uri("/api/v1/channels/verify")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"channelUsername\":\"\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error_code").isNotEmpty();
    }

    @Test
    @DisplayName("POST /api/v1/channels/verify without auth returns 403")
    void verifyWithoutAuth_returns403() {
        webClient.post()
                .uri("/api/v1/channels/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelVerifyRequest(CHAN_UNAME))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /api/v1/channels returns 201 on success")
    void registerSuccess_returns201() {
        String token = jwt(TEST_USER_ID);
        configureMockById();

        ChannelResponse body = webClient.post()
                .uri("/api/v1/channels")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelRegistrationRequest(
                        CHAN_TG_ID, List.of("tech"), null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ChannelResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(CHAN_TG_ID);
        assertThat(body.title()).isEqualTo(CHAN_TITLE);
        assertThat(body.ownerId()).isEqualTo(TEST_USER_ID);

        int dbCount = dsl.fetchCount(
                CHANNELS, CHANNELS.ID.eq(CHAN_TG_ID));
        assertThat(dbCount).isOne();
    }

    @Test
    @DisplayName("POST /api/v1/channels duplicate returns 409")
    void registerDuplicate_returns409() {
        String token = jwt(TEST_USER_ID);
        configureMockById();

        webClient.post()
                .uri("/api/v1/channels")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelRegistrationRequest(
                        CHAN_TG_ID, List.of("tech"), null))
                .exchange()
                .expectStatus().isCreated();

        webClient.post()
                .uri("/api/v1/channels")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelRegistrationRequest(
                        CHAN_TG_ID, List.of("tech"), null))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("CHANNEL_ALREADY_REGISTERED");
    }

    @Test
    @DisplayName("POST /api/v1/channels with bot not admin returns 403")
    void registerBotNotAdmin_returns403() {
        String token = jwt(TEST_USER_ID);

        when(telegramChannelPort.getChat(CHAN_TG_ID))
                .thenReturn(chatInfo());
        when(telegramChannelPort.getChatMember(CHAN_TG_ID, BOT_USER_ID))
                .thenReturn(member(BOT_USER_ID,
                        ChatMemberStatus.MEMBER));
        when(telegramChannelPort.getChatMember(
                CHAN_TG_ID, TEST_USER_ID))
                .thenReturn(admin(TEST_USER_ID));
        when(telegramChannelPort.getChatMemberCount(CHAN_TG_ID))
                .thenReturn(500);

        webClient.post()
                .uri("/api/v1/channels")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelRegistrationRequest(
                        CHAN_TG_ID, null, null))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("CHANNEL_BOT_NOT_ADMIN");
    }

    // --- helpers ---

    private String jwt(long userId) {
        return jwtTokenProvider.generateToken(
                new UserId(userId), false);
    }

    private void upsertUser(long userId) {
        dsl.insertInto(USERS)
                .set(USERS.ID, userId)
                .set(USERS.FIRST_NAME, "U")
                .set(USERS.LANGUAGE_CODE, "en")
                .onConflictDoNothing()
                .execute();
    }

    private void configureMockHappyPath() {
        when(telegramChannelPort.getChatByUsername(CHAN_UNAME))
                .thenReturn(chatInfo());
        when(telegramChannelPort.getChatMember(
                CHAN_TG_ID, BOT_USER_ID))
                .thenReturn(botAdmin());
        when(telegramChannelPort.getChatMember(
                CHAN_TG_ID, TEST_USER_ID))
                .thenReturn(admin(TEST_USER_ID));
        when(telegramChannelPort.getChatMemberCount(CHAN_TG_ID))
                .thenReturn(500);
    }

    private void configureMockById() {
        when(telegramChannelPort.getChat(CHAN_TG_ID))
                .thenReturn(chatInfo());
        when(telegramChannelPort.getChatMember(
                CHAN_TG_ID, BOT_USER_ID))
                .thenReturn(botAdmin());
        when(telegramChannelPort.getChatMember(
                CHAN_TG_ID, TEST_USER_ID))
                .thenReturn(admin(TEST_USER_ID));
        when(telegramChannelPort.getChatMemberCount(CHAN_TG_ID))
                .thenReturn(500);
    }

    private static ChatInfo chatInfo() {
        return new ChatInfo(CHAN_TG_ID, CHAN_TITLE,
                CHAN_UNAME, "channel", null);
    }

    private static ChatMemberInfo botAdmin() {
        return new ChatMemberInfo(BOT_USER_ID,
                ChatMemberStatus.ADMINISTRATOR,
                true, true, true, true);
    }

    private static ChatMemberInfo admin(long uid) {
        return new ChatMemberInfo(uid,
                ChatMemberStatus.CREATOR,
                true, true, true, true);
    }

    private static ChatMemberInfo member(long uid,
                                         ChatMemberStatus s) {
        return new ChatMemberInfo(uid, s,
                false, false, false, false);
    }

    /**
     * Minimal Spring config for channel HTTP tests.
     */
    @Configuration
    @EnableAutoConfiguration
    @EnableMethodSecurity
    @org.springframework.context.annotation.ComponentScan(basePackages = {
            "com.advertmarket.marketplace.channel.mapper",
            "com.advertmarket.marketplace.pricing.mapper"
    })
    static class TestConfig {

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
        JwtTokenProvider jwtTokenProvider(
                AuthProperties props) {
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
            return new LocalizationService(
                    new StaticMessageSource());
        }

        @Bean
        UserRepository userRepository(DSLContext dsl) {
            return new JooqUserRepository(dsl);
        }

        @Bean
        TokenBlacklistPort tokenBlacklistPort(
                StringRedisTemplate tpl) {
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
        UserBlockCheckPort userBlockCheckPort(
                StringRedisTemplate tpl) {
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
                            .requestMatchers("/api/v1/**")
                            .authenticated()
                            .anyRequest().denyAll())
                    .addFilterBefore(jwtFilter,
                            UsernamePasswordAuthenticationFilter
                                    .class)
                    .build();
        }

        @Bean
        TelegramChannelPort telegramChannelPort() {
            return mock(TelegramChannelPort.class);
        }

        @Bean
        ChannelBotProperties channelBotProperties() {
            return new ChannelBotProperties(BOT_USER_ID);
        }

        @Bean
        CategoryRepository categoryRepository(
                DSLContext dsl, JsonFacade jsonFacade) {
            return new JooqCategoryRepository(dsl, jsonFacade);
        }

        @Bean
        JooqPricingRuleRepository jooqPricingRuleRepository(
                DSLContext dsl,
                PricingRuleRecordMapper pricingRuleMapper) {
            return new JooqPricingRuleRepository(
                    dsl, pricingRuleMapper);
        }

        @Bean
        ChannelRepository channelRepository(
                DSLContext dsl,
                ChannelRecordMapper channelMapper,
                PricingRuleRecordMapper pricingRuleMapper,
                CategoryRepository categoryRepo,
                JooqPricingRuleRepository pricingRuleRepo) {
            return new JooqChannelRepository(
                    dsl, channelMapper, pricingRuleMapper,
                    categoryRepo, pricingRuleRepo);
        }

        @Bean
        ChannelVerificationService channelVerificationService(
                TelegramChannelPort tcp,
                ChannelBotProperties props) {
            return new ChannelVerificationService(tcp, props);
        }

        @Bean
        ChannelRegistrationService channelRegistrationService(
                ChannelVerificationService vs,
                ChannelRepository repo) {
            return new ChannelRegistrationService(vs, repo);
        }

        @Bean
        ChannelService channelService() {
            return mock(ChannelService.class);
        }

        @Bean
        ChannelController channelController(
                ChannelRegistrationService svc,
                ChannelService channelService) {
            return new ChannelController(svc, channelService);
        }

        @Bean
        TestExceptionHandler testExceptionHandler() {
            return new TestExceptionHandler();
        }
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
            addProps(pd, "VALIDATION_FAILED");
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
            pd.setTitle("Malformed body");
            pd.setDetail("Missing or invalid JSON");
            addProps(pd, "VALIDATION_FAILED");
            return ResponseEntity.badRequest().body(pd);
        }

        private void addProps(ProblemDetail pd, String ec) {
            pd.setProperty("error_code", ec);
            pd.setProperty("timestamp",
                    Instant.now().toString());
            pd.setProperty("correlation_id",
                    UUID.randomUUID().toString());
        }
    }
}

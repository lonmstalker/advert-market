package com.advertmarket.integration.communication;

import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.communication.api.notification.NotificationPort;
import com.advertmarket.communication.bot.handler.BotChannelStatusHandler;
import com.advertmarket.communication.bot.internal.block.RedisUserBlockService;
import com.advertmarket.communication.bot.internal.block.UserBlockPort;
import com.advertmarket.communication.bot.internal.block.UserBlockProperties;
import com.advertmarket.communication.bot.internal.config.TelegramBotProperties;
import com.advertmarket.communication.bot.internal.dispatch.BotCommand;
import com.advertmarket.communication.bot.internal.dispatch.BotDispatcher;
import com.advertmarket.communication.bot.internal.dispatch.CallbackHandler;
import com.advertmarket.communication.bot.internal.dispatch.ChatMemberUpdateHandler;
import com.advertmarket.communication.bot.internal.dispatch.HandlerRegistry;
import com.advertmarket.communication.bot.internal.dispatch.MessageHandler;
import com.advertmarket.communication.bot.internal.error.BotErrorHandler;
import com.advertmarket.communication.bot.internal.resilience.TelegramCircuitBreakerConfig;
import com.advertmarket.communication.bot.internal.resilience.TelegramResilienceProperties;
import com.advertmarket.communication.bot.internal.sender.RateLimiterPort;
import com.advertmarket.communication.bot.internal.sender.TelegramRateLimiter;
import com.advertmarket.communication.bot.internal.sender.TelegramRetryProperties;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.communication.bot.internal.sender.TelegramSenderProperties;
import com.advertmarket.communication.canary.CanaryRouter;
import com.advertmarket.communication.notification.TelegramNotificationService;
import com.advertmarket.communication.webhook.DeduplicationProperties;
import com.advertmarket.communication.webhook.TelegramWebhookController;
import com.advertmarket.communication.webhook.TelegramWebhookHandler;
import com.advertmarket.communication.webhook.UpdateDeduplicationPort;
import com.advertmarket.communication.webhook.UpdateDeduplicator;
import com.advertmarket.communication.webhook.UpdateProcessor;
import com.advertmarket.identity.adapter.JooqUserRepository;
import com.advertmarket.identity.config.LocaleCurrencyProperties;
import com.advertmarket.identity.mapper.UserProfileMapper;
import com.advertmarket.identity.service.LocaleCurrencyResolver;
import com.advertmarket.integration.support.ContainerProperties;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.RedisSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.marketplace.api.port.ChannelLifecyclePort;
import com.advertmarket.marketplace.channel.adapter.ChannelLifecycleAdapter;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.metric.MetricsFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.TelegramBot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jooq.autoconfigure.ExceptionTranslatorExecuteListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Full Telegram webhook flow integration:
 * secret validation, malformed payload, dedup, async dispatch,
 * channel lifecycle updates and owner notifications.
 *
 * <p>Only Telegram Bot API is mocked via local HTTP server.
 */
@SpringBootTest(
        classes = TelegramWebhookFlowIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DisplayName("Telegram webhook flow — full integration")
class TelegramWebhookFlowIntegrationTest {

    private static final String WEBHOOK_SECRET = "it-webhook-secret";
    private static final long OWNER_ID = 91L;
    private static final long CHANNEL_ID = -1002001L;
    private static final TelegramApiMockServer TELEGRAM_API =
            new TelegramApiMockServer();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        ContainerProperties.registerAll(registry);
        TELEGRAM_API.start();
    }

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
    }

    @AfterAll
    static void shutdownMockApi() {
        TELEGRAM_API.stop();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DSLContext dsl;

    private WebTestClient webClient;

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();
        DatabaseSupport.cleanAllTables(dsl);
        RedisSupport.flushAll();
        TELEGRAM_API.clear();
        TestDataFactory.upsertUser(dsl, OWNER_ID);
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);
    }

    @Test
    @DisplayName("POST /api/v1/bot/webhook returns 401 on invalid secret")
    void invalidSecretReturns401() throws Exception {
        webClient.post()
                .uri("/api/v1/bot/webhook")
                .header("X-Telegram-Bot-Api-Secret-Token", "wrong-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fixture("telegram/my_chat_member_admin_to_left.json"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/bot/webhook returns 400 on malformed payload")
    void malformedPayloadReturns400() {
        webClient.post()
                .uri("/api/v1/bot/webhook")
                .header("X-Telegram-Bot-Api-Secret-Token", WEBHOOK_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{not-json")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /api/v1/bot/webhook returns 413 on oversized payload")
    void oversizedPayloadReturns413() {
        String oversizedPayload = "{\"update_id\":1,\"payload\":\""
                + "x".repeat(300_000) + "\"}";
        webClient.post()
                .uri("/api/v1/bot/webhook")
                .header("X-Telegram-Bot-Api-Secret-Token", WEBHOOK_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(oversizedPayload)
                .exchange()
                .expectStatus().isEqualTo(413);
    }

    @Test
    @DisplayName("admin -> left deactivates channel and sends CHANNEL_BOT_REMOVED")
    void adminToLeftDeactivatesAndSendsRemovedNotification() throws Exception {
        webClient.post()
                .uri("/api/v1/bot/webhook")
                .header("X-Telegram-Bot-Api-Secret-Token", WEBHOOK_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fixture("telegram/my_chat_member_admin_to_left.json"))
                .exchange()
                .expectStatus().isOk();

        awaitChannelActive(false);
        awaitTelegramCalls(1);
    }

    @Test
    @DisplayName("Duplicate update_id does not trigger repeated side-effects")
    void duplicateUpdateIsDeduplicated() throws Exception {
        String payload = fixture("telegram/duplicate_update_id.json");

        webClient.post()
                .uri("/api/v1/bot/webhook")
                .header("X-Telegram-Bot-Api-Secret-Token", WEBHOOK_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isOk();

        webClient.post()
                .uri("/api/v1/bot/webhook")
                .header("X-Telegram-Bot-Api-Secret-Token", WEBHOOK_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isOk();

        awaitChannelActive(false);
        awaitTelegramCalls(1);
    }

    @Test
    @Tag("bot-hardening")
    @DisplayName("Burst duplicate updates produce only one side-effect")
    void burstDuplicatesProduceSingleSideEffect() throws Exception {
        String payload = fixture("telegram/duplicate_update_id.json");
        for (int i = 0; i < 20; i++) {
            webClient.post()
                    .uri("/api/v1/bot/webhook")
                    .header("X-Telegram-Bot-Api-Secret-Token", WEBHOOK_SECRET)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .exchange()
                    .expectStatus().isOk();
        }

        awaitChannelActive(false);
        awaitTelegramCalls(1);
    }

    @Test
    @DisplayName("member -> admin reactivates channel")
    void memberToAdminReactivatesChannel() throws Exception {
        dsl.update(CHANNELS)
                .set(CHANNELS.IS_ACTIVE, false)
                .where(CHANNELS.ID.eq(CHANNEL_ID))
                .execute();

        webClient.post()
                .uri("/api/v1/bot/webhook")
                .header("X-Telegram-Bot-Api-Secret-Token", WEBHOOK_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fixture("telegram/my_chat_member_member_to_admin.json"))
                .exchange()
                .expectStatus().isOk();

        awaitChannelActive(true);
        assertThat(TELEGRAM_API.requestCount()).isZero();
    }

    @Test
    @DisplayName("admin -> member deactivates channel and sends CHANNEL_BOT_DEMOTED")
    void adminToMemberDeactivatesAndSendsDemotedNotification() throws Exception {
        webClient.post()
                .uri("/api/v1/bot/webhook")
                .header("X-Telegram-Bot-Api-Secret-Token", WEBHOOK_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fixture("telegram/my_chat_member_admin_to_member.json"))
                .exchange()
                .expectStatus().isOk();

        awaitChannelActive(false);
        awaitTelegramCalls(1);
    }

    private void awaitChannelActive(boolean expected) {
        org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Boolean isActive = dsl.select(CHANNELS.IS_ACTIVE)
                            .from(CHANNELS)
                            .where(CHANNELS.ID.eq(CHANNEL_ID))
                            .fetchSingle(CHANNELS.IS_ACTIVE);
                    assertThat(isActive).isEqualTo(expected);
                });
    }

    private void awaitTelegramCalls(int expectedCount) {
        org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        assertThat(TELEGRAM_API.requestCount())
                                .isEqualTo(expectedCount));
        assertThat(TELEGRAM_API.requests())
                .allMatch(r -> r.path().contains("/sendMessage"));
    }

    private String fixture(String path) throws Exception {
        return new ClassPathResource(path)
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Configuration
    @EnableScheduling
    @EnableAutoConfiguration
    static class TestConfig {

        @Bean
        DSLContext dslContext(DataSource dataSource) {
            var configuration = new DefaultConfiguration()
                    .set(dataSource)
                    .set(SQLDialect.POSTGRES)
                    .set(new DefaultExecuteListenerProvider(
                            ExceptionTranslatorExecuteListener.DEFAULT));
            return DSL.using(configuration);
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/api/v1/bot/webhook").permitAll()
                            .anyRequest().denyAll())
                    .build();
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
        MessageSource messageSource() {
            var ms = new ReloadableResourceBundleMessageSource();
            ms.setBasenames(
                    "classpath:messages/bot",
                    "classpath:messages/notifications",
                    "classpath:messages/errors");
            ms.setDefaultEncoding("UTF-8");
            ms.setFallbackToSystemLocale(false);
            return ms;
        }

        @Bean
        LocalizationService localizationService(MessageSource messageSource) {
            return new LocalizationService(messageSource);
        }

        @Bean
        LocaleCurrencyProperties localeCurrencyProperties() {
            return new LocaleCurrencyProperties(
                    "USD",
                    Map.of(
                            "ru", "RUB",
                            "en", "USD"));
        }

        @Bean
        LocaleCurrencyResolver localeCurrencyResolver(
                LocaleCurrencyProperties properties) {
            return new LocaleCurrencyResolver(properties);
        }

        @Bean
        com.advertmarket.identity.api.port.UserRepository userRepository(
                DSLContext dsl,
                JsonFacade jsonFacade,
                LocaleCurrencyResolver localeCurrencyResolver) {
            return new JooqUserRepository(
                    dsl,
                    jsonFacade,
                    Mappers.getMapper(UserProfileMapper.class),
                    localeCurrencyResolver);
        }

        @Bean
        MetricsFacade metricsFacade() {
            return new MetricsFacade(new SimpleMeterRegistry());
        }

        @Bean
        TelegramBotProperties telegramBotProperties() {
            return new TelegramBotProperties(
                    "test-token",
                    "test_bot",
                    new TelegramBotProperties.Webhook(
                            "https://example.test/webhook",
                            WEBHOOK_SECRET,
                            262_144),
                    new TelegramBotProperties.WebApp("https://example.test"),
                    new TelegramBotProperties.Welcome(""));
        }

        @Bean
        TelegramBot telegramBot(TelegramBotProperties properties) {
            return new TelegramBot.Builder(properties.botToken())
                    .apiUrl(TELEGRAM_API.baseUrl())
                    .build();
        }

        @Bean
        TelegramSenderProperties telegramSenderProperties() {
            return new TelegramSenderProperties(
                    30,
                    1,
                    Duration.ofMinutes(5),
                    10_000,
                    1_000);
        }

        @Bean
        TelegramRetryProperties telegramRetryProperties() {
            return new TelegramRetryProperties(
                    3,
                    List.of(
                            Duration.ofSeconds(1),
                            Duration.ofSeconds(2),
                            Duration.ofSeconds(4)));
        }

        @Bean
        TelegramResilienceProperties telegramResilienceProperties() {
            return new TelegramResilienceProperties(
                    new TelegramResilienceProperties.CircuitBreaker(
                            50,
                            40,
                            Duration.ofSeconds(5),
                            Duration.ofSeconds(15),
                            10,
                            20),
                    new TelegramResilienceProperties.Bulkhead(
                            30,
                            Duration.ofSeconds(1)));
        }

        @Bean
        CircuitBreaker telegramCircuitBreaker(
                TelegramResilienceProperties properties) {
            return new TelegramCircuitBreakerConfig()
                    .telegramCircuitBreaker(properties);
        }

        @Bean
        Bulkhead telegramBulkhead(TelegramResilienceProperties properties) {
            return new TelegramCircuitBreakerConfig()
                    .telegramBulkhead(properties);
        }

        @Bean
        RateLimiterPort rateLimiterPort(TelegramSenderProperties properties) {
            return new TelegramRateLimiter(properties);
        }

        @Bean(name = "botUpdateExecutor", destroyMethod = "shutdown")
        ExecutorService botUpdateExecutor() {
            return Executors.newVirtualThreadPerTaskExecutor();
        }

        @Bean
        TelegramSender telegramSender(
                TelegramBot bot,
                RateLimiterPort rateLimiter,
                CircuitBreaker telegramCircuitBreaker,
                Bulkhead telegramBulkhead,
                MetricsFacade metricsFacade,
                TelegramRetryProperties retryProperties,
                ExecutorService botUpdateExecutor) {
            return new TelegramSender(
                    bot,
                    rateLimiter,
                    telegramCircuitBreaker,
                    telegramBulkhead,
                    metricsFacade,
                    retryProperties,
                    botUpdateExecutor);
        }

        @Bean
        UserBlockPort userBlockPort(StringRedisTemplate redisTemplate) {
            return new RedisUserBlockService(
                    redisTemplate, new UserBlockProperties("tg:block:"));
        }

        @Bean
        DeduplicationProperties deduplicationProperties() {
            return new DeduplicationProperties(Duration.ofHours(24));
        }

        @Bean
        UpdateDeduplicationPort updateDeduplicationPort(
                StringRedisTemplate redisTemplate,
                DeduplicationProperties deduplicationProperties,
                MetricsFacade metricsFacade) {
            return new UpdateDeduplicator(
                    redisTemplate,
                    deduplicationProperties,
                    metricsFacade);
        }

        @Bean
        CanaryRouter canaryRouter(
                StringRedisTemplate redisTemplate,
                MetricsFacade metricsFacade) {
            return new CanaryRouter(redisTemplate, metricsFacade);
        }

        @Bean
        ChannelLifecyclePort channelLifecyclePort(DSLContext dslContext) {
            return new ChannelLifecycleAdapter(dslContext);
        }

        @Bean
        NotificationPort notificationPort(
                TelegramSender sender,
                LocalizationService localizationService,
                com.advertmarket.identity.api.port.UserRepository
                        userRepository) {
            return new TelegramNotificationService(
                    sender, localizationService, userRepository);
        }

        @Bean
        BotChannelStatusHandler botChannelStatusHandler(
                ChannelLifecyclePort channelLifecyclePort,
                NotificationPort notificationPort,
                MetricsFacade metricsFacade) {
            return new BotChannelStatusHandler(
                    channelLifecyclePort,
                    notificationPort,
                    metricsFacade);
        }

        @Bean
        HandlerRegistry handlerRegistry(
                List<BotCommand> commands,
                List<CallbackHandler> callbacks,
                List<MessageHandler> messages,
                List<ChatMemberUpdateHandler> chatMemberHandlers) {
            return new HandlerRegistry(
                    commands, callbacks, messages, chatMemberHandlers);
        }

        @Bean
        BotErrorHandler botErrorHandler(
                MetricsFacade metricsFacade,
                TelegramSender telegramSender,
                LocalizationService localizationService) {
            return new BotErrorHandler(
                    metricsFacade,
                    telegramSender,
                    localizationService);
        }

        @Bean
        BotDispatcher botDispatcher(
                HandlerRegistry handlerRegistry,
                TelegramSender telegramSender,
                BotErrorHandler botErrorHandler,
                UserBlockPort userBlockPort,
                LocalizationService localizationService) {
            return new BotDispatcher(
                    handlerRegistry,
                    telegramSender,
                    botErrorHandler,
                    userBlockPort,
                    localizationService);
        }

        @Bean
        UpdateProcessor updateProcessor(
                CanaryRouter canaryRouter,
                BotDispatcher botDispatcher,
                BotErrorHandler botErrorHandler,
                MetricsFacade metricsFacade,
                ExecutorService botUpdateExecutor) {
            return new UpdateProcessor(
                    canaryRouter,
                    botDispatcher,
                    botErrorHandler,
                    metricsFacade,
                    botUpdateExecutor);
        }

        @Bean
        TelegramWebhookHandler telegramWebhookHandler(
                TelegramBotProperties telegramBotProperties,
                UpdateDeduplicationPort updateDeduplicationPort,
                UpdateProcessor updateProcessor,
                MetricsFacade metricsFacade) {
            return new TelegramWebhookHandler(
                    telegramBotProperties,
                    updateDeduplicationPort,
                    updateProcessor,
                    metricsFacade);
        }

        @Bean
        TelegramWebhookController telegramWebhookController(
                TelegramWebhookHandler telegramWebhookHandler) {
            return new TelegramWebhookController(telegramWebhookHandler);
        }
    }

    static final class TelegramApiMockServer {

        private static final String RESPONSE_BODY = """
                {"ok":true,"result":{"message_id":1,"date":0,"chat":{"id":91,"type":"private"}}}
                """;

        private com.sun.net.httpserver.HttpServer server;
        private final CopyOnWriteArrayList<TelegramRequest> requests =
                new CopyOnWriteArrayList<>();

        synchronized void start() {
            if (server != null) {
                return;
            }
            try {
                server = com.sun.net.httpserver.HttpServer.create(
                        new InetSocketAddress(0), 0);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Cannot start Telegram API mock server", e);
            }
            server.createContext("/bot", exchange -> {
                byte[] body = exchange.getRequestBody().readAllBytes();
                requests.add(new TelegramRequest(
                        exchange.getRequestURI().getPath(),
                        new String(body, StandardCharsets.UTF_8)));
                byte[] response = RESPONSE_BODY.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add(
                        "Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            });
            server.start();
        }

        synchronized void stop() {
            if (server != null) {
                server.stop(0);
                server = null;
            }
            requests.clear();
        }

        synchronized String baseUrl() {
            if (server == null) {
                throw new IllegalStateException("Mock server is not started");
            }
            return "http://localhost:" + server.getAddress().getPort() + "/bot";
        }

        void clear() {
            requests.clear();
        }

        int requestCount() {
            return requests.size();
        }

        List<TelegramRequest> requests() {
            return List.copyOf(requests);
        }
    }

    record TelegramRequest(String path, String body) {
    }
}

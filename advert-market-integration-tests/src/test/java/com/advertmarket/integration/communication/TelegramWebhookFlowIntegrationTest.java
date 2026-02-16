package com.advertmarket.integration.communication;

import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import com.advertmarket.communication.api.notification.NotificationPort;
import com.advertmarket.communication.api.notification.NotificationRequest;
import com.advertmarket.communication.api.notification.NotificationType;
import com.advertmarket.communication.bot.handler.BotChannelStatusHandler;
import com.advertmarket.communication.bot.internal.block.UserBlockPort;
import com.advertmarket.communication.bot.internal.config.TelegramBotProperties;
import com.advertmarket.communication.bot.internal.dispatch.BotDispatcher;
import com.advertmarket.communication.bot.internal.dispatch.HandlerRegistry;
import com.advertmarket.communication.bot.internal.error.BotErrorHandler;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.communication.canary.CanaryRouter;
import com.advertmarket.communication.webhook.DeduplicationProperties;
import com.advertmarket.communication.webhook.TelegramWebhookController;
import com.advertmarket.communication.webhook.TelegramWebhookHandler;
import com.advertmarket.communication.webhook.UpdateDeduplicationPort;
import com.advertmarket.communication.webhook.UpdateDeduplicator;
import com.advertmarket.communication.webhook.UpdateProcessor;
import com.advertmarket.integration.support.ContainerProperties;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.RedisSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.marketplace.api.port.ChannelLifecyclePort;
import com.advertmarket.marketplace.channel.adapter.ChannelLifecycleAdapter;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.metric.MetricsFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jooq.autoconfigure.ExceptionTranslatorExecuteListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Full Telegram webhook flow integration:
 * secret validation, malformed payload, dedup, async dispatch,
 * channel lifecycle updates and owner notifications.
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

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        ContainerProperties.registerAll(registry);
    }

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private NotificationCapture notificationCapture;

    private WebTestClient webClient;

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();
        DatabaseSupport.cleanAllTables(dsl);
        RedisSupport.flushAll();
        notificationCapture.clear();
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
    @DisplayName("admin -> left deactivates channel and sends CHANNEL_BOT_REMOVED")
    void adminToLeftDeactivatesAndSendsRemovedNotification() throws Exception {
        webClient.post()
                .uri("/api/v1/bot/webhook")
                .header("X-Telegram-Bot-Api-Secret-Token", WEBHOOK_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fixture("telegram/my_chat_member_admin_to_left.json"))
                .exchange()
                .expectStatus().isOk();

        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Boolean isActive = dsl.select(CHANNELS.IS_ACTIVE)
                            .from(CHANNELS)
                            .where(CHANNELS.ID.eq(CHANNEL_ID))
                            .fetchSingle(CHANNELS.IS_ACTIVE);
                    assertThat(isActive).isFalse();
                    assertThat(notificationCapture.requests()).hasSize(1);
                    assertThat(notificationCapture.requests().getFirst().type())
                            .isEqualTo(NotificationType.CHANNEL_BOT_REMOVED);
                });
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

        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Boolean isActive = dsl.select(CHANNELS.IS_ACTIVE)
                            .from(CHANNELS)
                            .where(CHANNELS.ID.eq(CHANNEL_ID))
                            .fetchSingle(CHANNELS.IS_ACTIVE);
                    assertThat(isActive).isFalse();
                    assertThat(notificationCapture.requests()).hasSize(1);
                });
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

        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Boolean isActive = dsl.select(CHANNELS.IS_ACTIVE)
                            .from(CHANNELS)
                            .where(CHANNELS.ID.eq(CHANNEL_ID))
                            .fetchSingle(CHANNELS.IS_ACTIVE);
                    assertThat(isActive).isTrue();
                });
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

        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Boolean isActive = dsl.select(CHANNELS.IS_ACTIVE)
                            .from(CHANNELS)
                            .where(CHANNELS.ID.eq(CHANNEL_ID))
                            .fetchSingle(CHANNELS.IS_ACTIVE);
                    assertThat(isActive).isFalse();
                    assertThat(notificationCapture.requests()).hasSize(1);
                    assertThat(notificationCapture.requests().getFirst().type())
                            .isEqualTo(NotificationType.CHANNEL_BOT_DEMOTED);
                });
    }

    private String fixture(String path) throws Exception {
        return new ClassPathResource(path)
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Configuration
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
        MetricsFacade metricsFacade() {
            return new MetricsFacade(new SimpleMeterRegistry());
        }

        @Bean
        LocalizationService localizationService() {
            return new LocalizationService(new StaticMessageSource());
        }

        @Bean
        TelegramBotProperties telegramBotProperties() {
            return new TelegramBotProperties(
                    "test-token",
                    "test_bot",
                    new TelegramBotProperties.Webhook(
                            "https://example.test/webhook",
                            WEBHOOK_SECRET),
                    new TelegramBotProperties.WebApp("https://example.test"),
                    new TelegramBotProperties.Welcome(""));
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

        @Bean(name = "botUpdateExecutor", destroyMethod = "shutdown")
        ExecutorService botUpdateExecutor() {
            return Executors.newSingleThreadExecutor();
        }

        @Bean
        TelegramSender telegramSender() {
            return mock(TelegramSender.class);
        }

        @Bean
        UserBlockPort userBlockPort() {
            return new UserBlockPort() {
                @Override
                public boolean isBlocked(long userId) {
                    return false;
                }

                @Override
                public void blockPermanently(long userId, String reason) {
                }

                @Override
                public void blockTemporarily(
                        long userId,
                        String reason,
                        Duration duration) {
                }

                @Override
                public void unblock(long userId) {
                }
            };
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
        NotificationCapture notificationCapture() {
            return new NotificationCapture();
        }

        @Bean
        NotificationPort notificationPort(NotificationCapture capture) {
            return capture;
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
                List<com.advertmarket.communication.bot.internal.dispatch.BotCommand> commands,
                List<com.advertmarket.communication.bot.internal.dispatch.CallbackHandler> callbacks,
                List<com.advertmarket.communication.bot.internal.dispatch.MessageHandler> messages,
                List<com.advertmarket.communication.bot.internal.dispatch.ChatMemberUpdateHandler> chatMemberHandlers) {
            return new HandlerRegistry(commands, callbacks, messages, chatMemberHandlers);
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

    static final class NotificationCapture implements NotificationPort {

        private final CopyOnWriteArrayList<NotificationRequest> requests =
                new CopyOnWriteArrayList<>();

        @Override
        public boolean send(NotificationRequest request) {
            requests.add(request);
            return true;
        }

        List<NotificationRequest> requests() {
            return List.copyOf(requests);
        }

        void clear() {
            requests.clear();
        }
    }
}


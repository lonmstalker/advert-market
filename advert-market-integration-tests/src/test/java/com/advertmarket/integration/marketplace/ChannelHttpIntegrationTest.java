package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.integration.marketplace.config.MarketplaceTestConfig;
import com.advertmarket.integration.support.ContainerProperties;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.marketplace.api.dto.ChannelRegistrationRequest;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelVerifyRequest;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberStatus;
import com.advertmarket.marketplace.api.port.CategoryRepository;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.marketplace.channel.config.ChannelBotProperties;
import com.advertmarket.marketplace.channel.mapper.CategoryDtoMapper;
import com.advertmarket.marketplace.channel.repository.JooqCategoryRepository;
import com.advertmarket.marketplace.channel.repository.JooqChannelRepository;
import com.advertmarket.marketplace.channel.service.ChannelRegistrationService;
import com.advertmarket.marketplace.channel.service.ChannelRegistrationTxService;
import com.advertmarket.marketplace.channel.service.ChannelService;
import com.advertmarket.marketplace.channel.service.ChannelAutoSyncService;
import com.advertmarket.marketplace.channel.service.ChannelVerificationService;
import com.advertmarket.marketplace.channel.web.ChannelController;
import com.advertmarket.marketplace.channel.web.ChannelSearchCriteriaConverter;
import com.advertmarket.marketplace.pricing.repository.JooqPricingRuleRepository;
import com.advertmarket.shared.json.JsonFacade;
import java.time.Duration;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * HTTP-level integration tests for Channel endpoints.
 */
@SpringBootTest(
        classes = ChannelHttpIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DisplayName("Channel HTTP — end-to-end integration")
class ChannelHttpIntegrationTest {

    private static final long TEST_USER_ID = 1L;
    private static final long BOT_USER_ID = 2L;
    private static final long CHAN_TG_ID = -100L;
    private static final String CHAN_TITLE = "Chan A";
    private static final String CHAN_UNAME = "chan_a";

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry) {
        ContainerProperties.registerAll(registry);
    }

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
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
        DatabaseSupport.cleanAllTables(dsl);
        TestDataFactory.upsertUser(dsl, TEST_USER_ID);
        configureMockHappyPath();
    }

    @Test
    @DisplayName("POST /api/v1/channels/verify returns 200 on success")
    void verifySuccess_returns200() {
        String token = TestDataFactory.jwt(
                jwtTokenProvider, TEST_USER_ID);

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
        String token = TestDataFactory.jwt(
                jwtTokenProvider, TEST_USER_ID);

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
        String token = TestDataFactory.jwt(
                jwtTokenProvider, TEST_USER_ID);
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
        String token = TestDataFactory.jwt(
                jwtTokenProvider, TEST_USER_ID);
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
        String token = TestDataFactory.jwt(
                jwtTokenProvider, TEST_USER_ID);

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

    private void configureMockHappyPath() {
        when(telegramChannelPort.getChatByUsername(CHAN_UNAME))
                .thenReturn(chatInfo());
        when(telegramChannelPort.getChat(CHAN_TG_ID))
                .thenReturn(chatInfo());
        when(telegramChannelPort.getChatMember(
                CHAN_TG_ID, BOT_USER_ID))
                .thenReturn(botAdmin());
        when(telegramChannelPort.getChatMember(
                CHAN_TG_ID, TEST_USER_ID))
                .thenReturn(admin(TEST_USER_ID));
        when(telegramChannelPort.getChatAdministrators(CHAN_TG_ID))
                .thenReturn(List.of(admin(TEST_USER_ID), botAdmin()));
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
    @Import(MarketplaceTestConfig.class)
    @ComponentScan(basePackages = {
            "com.advertmarket.marketplace.channel.mapper",
            "com.advertmarket.marketplace.pricing.mapper"
    })
    static class TestConfig {

        @Bean
        TelegramChannelPort telegramChannelPort() {
            return mock(TelegramChannelPort.class);
        }

        @Bean
        ChannelBotProperties channelBotProperties() {
            return new ChannelBotProperties(
                    BOT_USER_ID, Duration.ofSeconds(3));
        }

        @Bean
        CategoryRepository categoryRepository(
                DSLContext dsl,
                JsonFacade jsonFacade,
                CategoryDtoMapper categoryDtoMapper) {
            return new JooqCategoryRepository(
                    dsl, jsonFacade, categoryDtoMapper);
        }

        @Bean
        JooqPricingRuleRepository jooqPricingRuleRepository(
                DSLContext dsl,
                com.advertmarket.marketplace.pricing.mapper
                        .PricingRuleRecordMapper pricingRuleMapper) {
            return new JooqPricingRuleRepository(
                    dsl, pricingRuleMapper);
        }

        @Bean
        ChannelRepository channelRepository(
                DSLContext dsl,
                com.advertmarket.marketplace.channel.mapper
                        .ChannelRecordMapper channelMapper,
                CategoryRepository categoryRepo,
                JooqPricingRuleRepository pricingRuleRepo) {
            return new JooqChannelRepository(
                    dsl, channelMapper,
                    categoryRepo, pricingRuleRepo);
        }

        @Bean
        ChannelVerificationService channelVerificationService(
                TelegramChannelPort tcp,
                ChannelBotProperties props) {
            return new ChannelVerificationService(
                    tcp, props, Runnable::run);
        }

        @Bean
        ChannelRegistrationTxService channelRegistrationTxService(
                ChannelRepository repo) {
            return new ChannelRegistrationTxService(repo);
        }

        @Bean
        ChannelRegistrationService channelRegistrationService(
                ChannelVerificationService vs,
                ChannelAutoSyncService autoSyncService,
                ChannelRepository repo,
                ChannelRegistrationTxService txService) {
            return new ChannelRegistrationService(
                    vs, autoSyncService, repo, txService);
        }

        @Bean
        ChannelAutoSyncService channelAutoSyncService(
                TelegramChannelPort telegramChannelPort,
                DSLContext dsl) {
            return new ChannelAutoSyncService(telegramChannelPort, dsl);
        }

        @Bean
        ChannelService channelService() {
            return mock(ChannelService.class);
        }

        @Bean
        ChannelSearchCriteriaConverter channelSearchCriteriaConverter() {
            return new ChannelSearchCriteriaConverter();
        }

        @Bean
        ChannelController channelController(
                ChannelRegistrationService svc,
                ChannelService channelService,
                ChannelSearchCriteriaConverter converter) {
            return new ChannelController(svc, channelService, converter);
        }
    }
}

package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.Categories.CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
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
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.dto.ChannelVerifyRequest;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberStatus;
import com.advertmarket.marketplace.api.port.CategoryRepository;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.api.port.ChannelSearchPort;
import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.marketplace.channel.adapter.ChannelAuthorizationAdapter;
import com.advertmarket.marketplace.channel.config.ChannelBotProperties;
import com.advertmarket.marketplace.channel.mapper.CategoryDtoMapper;
import com.advertmarket.marketplace.channel.mapper.ChannelListItemMapper;
import com.advertmarket.marketplace.channel.repository.JooqCategoryRepository;
import com.advertmarket.marketplace.channel.repository.JooqChannelRepository;
import com.advertmarket.marketplace.channel.search.ParadeDbChannelSearch;
import com.advertmarket.marketplace.channel.service.ChannelAutoSyncService;
import com.advertmarket.marketplace.channel.service.ChannelRegistrationService;
import com.advertmarket.marketplace.channel.service.ChannelRegistrationTxService;
import com.advertmarket.marketplace.channel.service.ChannelService;
import com.advertmarket.marketplace.channel.service.ChannelVerificationService;
import com.advertmarket.marketplace.channel.web.ChannelController;
import com.advertmarket.marketplace.channel.web.ChannelSearchCriteriaConverter;
import com.advertmarket.marketplace.pricing.repository.JooqPricingRuleRepository;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.pagination.CursorPage;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration test for full channel registration flow:
 * verify → register → search → detail → my channels.
 */
@SpringBootTest(
        classes = ChannelRegistrationFlowIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DisplayName("Channel registration flow — verify → register → search → my")
class ChannelRegistrationFlowIntegrationTest {

    private static final long TEST_USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long BOT_USER_ID = 2L;
    private static final long CHAN_TG_ID = -100L;
    private static final String CHAN_TITLE = "Test Registration Channel";
    private static final String CHAN_UNAME = "test_reg_chan";

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
                .responseTimeout(Duration.ofSeconds(10))
                .build();
        reset(telegramChannelPort);
        DatabaseSupport.cleanAllTables(dsl);
        TestDataFactory.upsertUser(dsl, TEST_USER_ID);
        TestDataFactory.upsertUser(dsl, OTHER_USER_ID);
        configureMocks();
    }

    @Test
    @DisplayName("Registered channel appears in search results")
    void registeredChannelAppearsInSearchResults() {
        String token = TestDataFactory.jwt(jwtTokenProvider, TEST_USER_ID);
        registerChannel(token);

        var result = webClient.get()
                .uri("/api/v1/channels?limit=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CursorPageBody.class)
                .returnResult()
                .getResponseBody();

        assertThat(result).isNotNull();
        assertThat(result.items()).isNotEmpty();
        assertThat(result.items())
                .anyMatch(ch -> ch.title().equals(CHAN_TITLE));
    }

    @Test
    @DisplayName("Registered channel filtered by category")
    void registeredChannelFilteredByCategory() {
        String token = TestDataFactory.jwt(jwtTokenProvider, TEST_USER_ID);
        registerChannel(token);

        var techResult = webClient.get()
                .uri("/api/v1/channels?category=tech&limit=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CursorPageBody.class)
                .returnResult()
                .getResponseBody();

        assertThat(techResult).isNotNull();
        assertThat(techResult.items())
                .anyMatch(ch -> ch.title().equals(CHAN_TITLE));

        var cryptoResult = webClient.get()
                .uri("/api/v1/channels?category=crypto&limit=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CursorPageBody.class)
                .returnResult()
                .getResponseBody();

        assertThat(cryptoResult).isNotNull();
        assertThat(cryptoResult.items())
                .noneMatch(ch -> ch.title().equals(CHAN_TITLE));
    }

    @Test
    @DisplayName("My channels returns all channels where user is a member")
    void myChannelsReturnsOwnedChannels() {
        String ownerToken = TestDataFactory.jwt(
                jwtTokenProvider, TEST_USER_ID);
        verifyChannel(ownerToken);

        var myChannels = webClient.get()
                .uri("/api/v1/channels/my")
                .headers(h -> h.setBearerAuth(ownerToken))
                .exchange()
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<
                            List<ChannelResponse>>() {})
                    .returnResult()
                    .getResponseBody();

        assertThat(myChannels).isNotNull();
        assertThat(myChannels).hasSize(1);
        assertThat(myChannels.getFirst().title()).isEqualTo(CHAN_TITLE);
        assertThat(myChannels.getFirst().ownerId())
                .isEqualTo(TEST_USER_ID);

        String otherToken = TestDataFactory.jwt(
                jwtTokenProvider, OTHER_USER_ID);
        var otherChannels = webClient.get()
                .uri("/api/v1/channels/my")
                .headers(h -> h.setBearerAuth(otherToken))
                .exchange()
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<
                            List<ChannelResponse>>() {})
                    .returnResult()
                    .getResponseBody();

        assertThat(otherChannels).isNotNull();
        assertThat(otherChannels).hasSize(1);
        assertThat(otherChannels.getFirst().title()).isEqualTo(CHAN_TITLE);
    }

    @Test
    @DisplayName("Owner transfer via Telegram sync revokes old owner and grants new owner")
    void ownerTransferViaSyncUpdatesAccessAndMemberships() {
        String oldOwnerToken = TestDataFactory.jwt(
                jwtTokenProvider, TEST_USER_ID);
        String newOwnerToken = TestDataFactory.jwt(
                jwtTokenProvider, OTHER_USER_ID);

        verifyChannel(oldOwnerToken);

        when(telegramChannelPort.getChatAdministrators(CHAN_TG_ID))
                .thenReturn(List.of(admin(OTHER_USER_ID), botAdmin()));
        when(telegramChannelPort.getChatMemberCount(CHAN_TG_ID))
                .thenReturn(777);

        webClient.put()
                .uri("/api/v1/channels/{id}", CHAN_TG_ID)
                .headers(h -> h.setBearerAuth(oldOwnerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelUpdateRequest(
                        "old owner update", null, null, null, null))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("CHANNEL_NOT_OWNED");

        webClient.put()
                .uri("/api/v1/channels/{id}", CHAN_TG_ID)
                .headers(h -> h.setBearerAuth(newOwnerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelUpdateRequest(
                        "new owner update", null, null, null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ownerId")
                .isEqualTo(OTHER_USER_ID);

        Long ownerId = dsl.select(CHANNELS.OWNER_ID)
                .from(CHANNELS)
                .where(CHANNELS.ID.eq(CHAN_TG_ID))
                .fetchOne(CHANNELS.OWNER_ID);
        assertThat(ownerId).isEqualTo(OTHER_USER_ID);

        int ownerMembershipCount = dsl.selectCount()
                .from(CHANNEL_MEMBERSHIPS)
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(CHAN_TG_ID))
                .and(CHANNEL_MEMBERSHIPS.ROLE.eq("OWNER"))
                .fetchOne(0, int.class);
        assertThat(ownerMembershipCount).isEqualTo(1);

        Long membershipOwnerId = dsl.select(CHANNEL_MEMBERSHIPS.USER_ID)
                .from(CHANNEL_MEMBERSHIPS)
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(CHAN_TG_ID))
                .and(CHANNEL_MEMBERSHIPS.ROLE.eq("OWNER"))
                .fetchOne(CHANNEL_MEMBERSHIPS.USER_ID);
        assertThat(membershipOwnerId).isEqualTo(OTHER_USER_ID);

        var oldOwnerChannels = webClient.get()
                .uri("/api/v1/channels/my")
                .headers(h -> h.setBearerAuth(oldOwnerToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<
                        List<ChannelResponse>>() {})
                .returnResult()
                .getResponseBody();
        assertThat(oldOwnerChannels).isEmpty();

        var newOwnerChannels = webClient.get()
                .uri("/api/v1/channels/my")
                .headers(h -> h.setBearerAuth(newOwnerToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<
                        List<ChannelResponse>>() {})
                .returnResult()
                .getResponseBody();
        assertThat(newOwnerChannels).hasSize(1);
        assertThat(newOwnerChannels.getFirst().ownerId())
                .isEqualTo(OTHER_USER_ID);
    }

    @Test
    @DisplayName("Full flow: verify → register → search → detail")
    void fullFlowVerifyRegisterSearchDetail() {
        String token = TestDataFactory.jwt(jwtTokenProvider, TEST_USER_ID);

        // 1. Verify
        ChannelVerifyResponse verifyBody = webClient.post()
                .uri("/api/v1/channels/verify")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelVerifyRequest(CHAN_UNAME))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChannelVerifyResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(verifyBody).isNotNull();
        assertThat(verifyBody.channelId()).isEqualTo(CHAN_TG_ID);
        assertThat(verifyBody.botStatus().isAdmin()).isTrue();

        // 2. Register fallback after successful autosync
        webClient.post()
                .uri("/api/v1/channels")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelRegistrationRequest(
                        CHAN_TG_ID, List.of("tech"), null))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("CHANNEL_ALREADY_REGISTERED");

        // 3. Search — should find
        var searchResult = webClient.get()
                .uri("/api/v1/channels?limit=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CursorPageBody.class)
                .returnResult()
                .getResponseBody();

        assertThat(searchResult).isNotNull();
        assertThat(searchResult.items())
                .anyMatch(ch -> ch.title().equals(CHAN_TITLE));

        // 4. Detail
        webClient.get()
                .uri("/api/v1/channels/{id}", CHAN_TG_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(CHAN_TG_ID)
                .jsonPath("$.title").isEqualTo(CHAN_TITLE)
                .jsonPath("$.ownerId").isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("My channels without auth returns 403")
    void myChannelsWithoutAuth_returns403() {
        webClient.get()
                .uri("/api/v1/channels/my")
                .exchange()
                .expectStatus().isForbidden();
    }

    // --- helpers ---

    private void registerChannel(String token) {
        configureMockById();
        webClient.post()
                .uri("/api/v1/channels")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelRegistrationRequest(
                        CHAN_TG_ID, List.of("tech"), null))
                .exchange()
                .expectStatus().isCreated();
    }

    private void verifyChannel(String token) {
        webClient.post()
                .uri("/api/v1/channels/verify")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelVerifyRequest(CHAN_UNAME))
                .exchange()
                .expectStatus().isOk();
    }

    private void configureMocks() {
        when(telegramChannelPort.getChatByUsername(CHAN_UNAME))
                .thenReturn(chatInfo());
        when(telegramChannelPort.getChat(CHAN_TG_ID))
                .thenReturn(chatInfo());
        when(telegramChannelPort.getChatMember(CHAN_TG_ID, BOT_USER_ID))
                .thenReturn(botAdmin());
        when(telegramChannelPort.getChatMember(CHAN_TG_ID, TEST_USER_ID))
                .thenReturn(admin(TEST_USER_ID));
        when(telegramChannelPort.getChatAdministrators(CHAN_TG_ID))
                .thenReturn(List.of(admin(TEST_USER_ID), botAdmin()));
        when(telegramChannelPort.getChatMemberCount(CHAN_TG_ID))
                .thenReturn(500);
    }

    private void configureMockById() {
        when(telegramChannelPort.getChat(CHAN_TG_ID))
                .thenReturn(chatInfo());
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

    record CursorPageItem(long id, String title, String username) {}

    record CursorPageBody(List<CursorPageItem> items, String nextCursor) {}

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
                CategoryDtoMapper mapper) {
            return new JooqCategoryRepository(dsl, jsonFacade, mapper);
        }

        @Bean
        JooqPricingRuleRepository jooqPricingRuleRepository(
                DSLContext dsl,
                com.advertmarket.marketplace.pricing.mapper
                        .PricingRuleRecordMapper pricingRuleMapper) {
            return new JooqPricingRuleRepository(dsl, pricingRuleMapper);
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
        ChannelSearchPort channelSearchPort(
                DSLContext dsl,
                CategoryRepository categoryRepo,
                ChannelListItemMapper channelListItemMapper) {
            return new ParadeDbChannelSearch(
                    dsl, categoryRepo, channelListItemMapper);
        }

        @Bean
        ChannelVerificationService channelVerificationService(
                TelegramChannelPort tcp,
                ChannelBotProperties props) {
            return new ChannelVerificationService(
                    tcp, props, Runnable::run);
        }

        @Bean
        ChannelAutoSyncService channelAutoSyncService(
                TelegramChannelPort tcp,
                DSLContext dsl) {
            return new ChannelAutoSyncService(tcp, dsl);
        }

        @Bean
        ChannelRegistrationTxService channelRegistrationTxService(
                ChannelRepository repo) {
            return new ChannelRegistrationTxService(repo);
        }

        @Bean
        ChannelRegistrationService channelRegistrationService(
                ChannelVerificationService vs,
                ChannelRepository repo,
                ChannelRegistrationTxService txService) {
            return new ChannelRegistrationService(
                    vs, repo, txService);
        }

        @Bean
        ChannelAuthorizationAdapter channelAuthorizationAdapter(
                DSLContext dsl) {
            return new ChannelAuthorizationAdapter(dsl);
        }

        @Bean
        ChannelService channelService(
                ChannelSearchPort searchPort,
                ChannelRepository channelRepo,
                ChannelAuthorizationAdapter authAdapter,
                ChannelAutoSyncService autoSyncService) {
            return new ChannelService(
                    searchPort, channelRepo, authAdapter, autoSyncService);
        }

        @Bean
        ChannelSearchCriteriaConverter channelSearchCriteriaConverter() {
            return new ChannelSearchCriteriaConverter();
        }

        @Bean
        ChannelController channelController(
                ChannelRegistrationService regSvc,
                ChannelService channelService,
                ChannelSearchCriteriaConverter criteriaConverter) {
            return new ChannelController(
                    regSvc, channelService, criteriaConverter);
        }
    }
}

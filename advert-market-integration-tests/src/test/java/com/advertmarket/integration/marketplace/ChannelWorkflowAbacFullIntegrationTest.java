package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.Categories.CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;
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
import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberStatus;
import com.advertmarket.marketplace.api.port.CategoryRepository;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.api.port.ChannelSearchPort;
import com.advertmarket.marketplace.api.port.TeamMembershipRepository;
import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.marketplace.channel.adapter.ChannelAuthorizationAdapter;
import com.advertmarket.marketplace.channel.config.ChannelBotProperties;
import com.advertmarket.marketplace.channel.mapper.CategoryDtoMapper;
import com.advertmarket.marketplace.channel.mapper.ChannelListItemMapper;
import com.advertmarket.marketplace.channel.mapper.ChannelRecordMapper;
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
import com.advertmarket.marketplace.pricing.mapper.PricingRuleRecordMapper;
import com.advertmarket.marketplace.pricing.repository.JooqPricingRuleRepository;
import com.advertmarket.marketplace.pricing.service.PricingRuleService;
import com.advertmarket.marketplace.pricing.web.PricingRuleController;
import com.advertmarket.marketplace.team.config.TeamProperties;
import com.advertmarket.marketplace.team.mapper.TeamMemberDtoMapper;
import com.advertmarket.marketplace.team.repository.JooqTeamMembershipRepository;
import com.advertmarket.marketplace.team.service.TeamService;
import com.advertmarket.marketplace.team.web.TeamController;
import com.advertmarket.shared.json.JsonFacade;
import java.time.Duration;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
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
 * End-to-end ABAC workflow test:
 * verify -> register -> invite manager with rights -> manager manages
 * channel/pricing -> marketplace retrieval endpoints reflect updates.
 */
@SpringBootTest(
        classes = ChannelWorkflowAbacFullIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DisplayName("Channel workflow ABAC full flow — integration")
class ChannelWorkflowAbacFullIntegrationTest {

    private static final long OWNER_ID = 11L;
    private static final long MANAGER_ID = 12L;
    private static final long OTHER_OWNER_ID = 13L;
    private static final long BOT_USER_ID = 1002L;

    private static final long CHANNEL_ID = -10001001L;
    private static final String CHANNEL_USERNAME = "abac_flow_main";
    private static final String CHANNEL_TITLE = "ABAC Flow Main";

    private static final long SECOND_CHANNEL_ID = -10001002L;

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
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private TelegramChannelPort telegramChannelPort;

    private WebTestClient webClient;

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();
        reset(telegramChannelPort);
        DatabaseSupport.cleanAllTables(dsl);
        TestDataFactory.upsertUser(dsl, OWNER_ID);
        TestDataFactory.upsertUser(dsl, MANAGER_ID);
        TestDataFactory.upsertUser(dsl, OTHER_OWNER_ID);
        configureTelegramMocks();
    }

    @Test
    @DisplayName("Full flow: manager with MANAGE_LISTINGS manages channel, pricing, owner note and data visible in marketplace")
    void fullFlowManagerWithManageListings() {
        String ownerToken = jwt(OWNER_ID);
        String managerToken = jwt(MANAGER_ID);

        // 1) Verify + register channel
        webClient.post()
                .uri("/api/v1/channels/verify")
                .headers(h -> h.setBearerAuth(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelVerifyRequest(CHANNEL_USERNAME))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.channelId").isEqualTo(CHANNEL_ID)
                .jsonPath("$.botStatus.isAdmin").isEqualTo(true);

        webClient.post()
                .uri("/api/v1/channels")
                .headers(h -> h.setBearerAuth(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelRegistrationRequest(
                        CHANNEL_ID, List.of("tech"), 9_000_000L))
                .exchange()
                .expectStatus().isCreated();

        webClient.post()
                .uri("/api/v1/channels")
                .headers(h -> h.setBearerAuth(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelRegistrationRequest(
                        CHANNEL_ID, List.of("tech"), 9_000_000L))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("CHANNEL_ALREADY_REGISTERED");

        // 2) Invite manager with MANAGE_LISTINGS right via team endpoint
        webClient.post()
                .uri("/api/v1/channels/{channelId}/team", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "userId": %d,
                          "rights": ["MANAGE_LISTINGS", "MANAGE_TEAM"]
                        }
                        """.formatted(MANAGER_ID))
                .exchange()
                .expectStatus().isCreated();

        // 3) Manager updates channel, including owner note
        webClient.put()
                .uri("/api/v1/channels/{channelId}", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(managerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "Managed by manager",
                          "language": "en",
                          "pricePerPostNano": 11000000,
                          "customRules": "No casino ads"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.description").isEqualTo("Managed by manager");

        // 4) Manager creates pricing rule
        webClient.post()
                .uri("/api/v1/channels/{channelId}/pricing", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(managerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "Sponsored native",
                          "description": "Manager-defined rule",
                          "postTypes": ["NATIVE"],
                          "priceNano": 12000000,
                          "sortOrder": 1
                        }
                        """)
                .exchange()
                .expectStatus().isCreated();

        // 5) Seed one more active channel to validate cursor pagination/filtering
        TestDataFactory.insertChannelWithOwner(dsl, SECOND_CHANNEL_ID, OTHER_OWNER_ID);
        dsl.update(CHANNELS)
                .set(CHANNELS.TITLE, "Other Owner Channel")
                .set(CHANNELS.SUBSCRIBER_COUNT, 2_000)
                .set(CHANNELS.PRICE_PER_POST_NANO, 1_000_000L)
                .where(CHANNELS.ID.eq(SECOND_CHANNEL_ID))
                .execute();
        Integer techCategoryId = dsl.select(CATEGORIES.ID)
                .from(CATEGORIES)
                .where(CATEGORIES.SLUG.eq("tech"))
                .fetchSingle(CATEGORIES.ID);
        dsl.insertInto(CHANNEL_CATEGORIES)
                .set(CHANNEL_CATEGORIES.CHANNEL_ID, SECOND_CHANNEL_ID)
                .set(CHANNEL_CATEGORIES.CATEGORY_ID, techCategoryId)
                .execute();

        // 6) Marketplace list reflects updated data with filters
        webClient.get()
                .uri("/api/v1/channels?category=tech&minSubs=10000&maxSubs=20000&minPrice=10000000&maxPrice=13000000&limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items[0].id").isEqualTo(CHANNEL_ID);

        webClient.get()
                .uri("/api/v1/channels?limit=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.hasNext").isEqualTo(true)
                .jsonPath("$.nextCursor").isNotEmpty();

        // 7) Detail shows pricing rules + owner note
        webClient.get()
                .uri("/api/v1/channels/{channelId}", CHANNEL_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pricingRules.length()").isEqualTo(1)
                .jsonPath("$.pricingRules[0].name").isEqualTo("Sponsored native")
                .jsonPath("$.rules.customRules").isEqualTo("No casino ads");

        // 8) /my returns channel for owner and manager
        var ownerChannels = webClient.get()
                .uri("/api/v1/channels/my")
                .headers(h -> h.setBearerAuth(ownerToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<
                        List<ChannelResponse>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(ownerChannels).isNotNull();
        assertThat(ownerChannels.stream()
                .map(ChannelResponse::id))
                .contains(CHANNEL_ID);

        var managerChannels = webClient.get()
                .uri("/api/v1/channels/my")
                .headers(h -> h.setBearerAuth(managerToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<
                        List<ChannelResponse>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(managerChannels).isNotNull();
        assertThat(managerChannels.stream()
                .map(ChannelResponse::id))
                .contains(CHANNEL_ID);
    }

    private void configureTelegramMocks() {
        when(telegramChannelPort.getChatByUsername(CHANNEL_USERNAME))
                .thenReturn(new ChatInfo(
                        CHANNEL_ID,
                        CHANNEL_TITLE,
                        CHANNEL_USERNAME,
                        "channel",
                        null));
        when(telegramChannelPort.getChat(CHANNEL_ID))
                .thenReturn(new ChatInfo(
                        CHANNEL_ID,
                        CHANNEL_TITLE,
                        CHANNEL_USERNAME,
                        "channel",
                        null));
        when(telegramChannelPort.getChatMember(CHANNEL_ID, BOT_USER_ID))
                .thenReturn(new ChatMemberInfo(
                        BOT_USER_ID,
                        ChatMemberStatus.ADMINISTRATOR,
                        true,
                        true,
                        true,
                        true));
        when(telegramChannelPort.getChatMember(CHANNEL_ID, OWNER_ID))
                .thenReturn(new ChatMemberInfo(
                        OWNER_ID,
                        ChatMemberStatus.CREATOR,
                        true,
                        true,
                        true,
                        true));
        when(telegramChannelPort.getChatAdministrators(CHANNEL_ID))
                .thenReturn(List.of(
                        new ChatMemberInfo(
                                OWNER_ID,
                                ChatMemberStatus.CREATOR,
                                true,
                                true,
                                true,
                                true),
                        new ChatMemberInfo(
                                BOT_USER_ID,
                                ChatMemberStatus.ADMINISTRATOR,
                                true,
                                true,
                                true,
                                true)));
        when(telegramChannelPort.getChatMemberCount(CHANNEL_ID))
                .thenReturn(12_500);
    }

    private String jwt(long userId) {
        return TestDataFactory.jwt(jwtTokenProvider, userId);
    }

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
                PricingRuleRecordMapper mapper) {
            return new JooqPricingRuleRepository(dsl, mapper);
        }

        @Bean
        ChannelRepository channelRepository(
                DSLContext dsl,
                ChannelRecordMapper mapper,
                CategoryRepository categoryRepository,
                JooqPricingRuleRepository pricingRuleRepository) {
            return new JooqChannelRepository(
                    dsl,
                    mapper,
                    categoryRepository,
                    pricingRuleRepository);
        }

        @Bean
        ChannelSearchPort channelSearchPort(
                DSLContext dsl,
                CategoryRepository categoryRepository,
                ChannelListItemMapper channelListItemMapper) {
            return new ParadeDbChannelSearch(
                    dsl,
                    categoryRepository,
                    channelListItemMapper);
        }

        @Bean
        ChannelVerificationService channelVerificationService(
                TelegramChannelPort telegramChannelPort,
                ChannelBotProperties channelBotProperties) {
            return new ChannelVerificationService(
                    telegramChannelPort,
                    channelBotProperties,
                    Runnable::run);
        }

        @Bean
        ChannelAutoSyncService channelAutoSyncService(
                TelegramChannelPort telegramChannelPort,
                DSLContext dsl) {
            return new ChannelAutoSyncService(
                    telegramChannelPort,
                    dsl);
        }

        @Bean
        ChannelRegistrationTxService channelRegistrationTxService(
                ChannelRepository channelRepository) {
            return new ChannelRegistrationTxService(channelRepository);
        }

        @Bean
        ChannelRegistrationService channelRegistrationService(
                ChannelVerificationService channelVerificationService,
                ChannelRepository channelRepository,
                ChannelRegistrationTxService txService) {
            return new ChannelRegistrationService(
                    channelVerificationService,
                    channelRepository,
                    txService);
        }

        @Bean
        ChannelAuthorizationPort channelAuthorizationPort(DSLContext dsl) {
            return new ChannelAuthorizationAdapter(dsl);
        }

        @Bean
        ChannelService channelService(
                ChannelSearchPort channelSearchPort,
                ChannelRepository channelRepository,
                ChannelAuthorizationPort channelAuthorizationPort,
                ChannelAutoSyncService channelAutoSyncService) {
            return new ChannelService(
                    channelSearchPort,
                    channelRepository,
                    channelAuthorizationPort,
                    channelAutoSyncService);
        }

        @Bean
        TeamMembershipRepository teamMembershipRepository(
                DSLContext dsl,
                JsonFacade jsonFacade) {
            return new JooqTeamMembershipRepository(
                    dsl,
                    jsonFacade,
                    Mappers.getMapper(TeamMemberDtoMapper.class));
        }

        @Bean
        TeamProperties teamProperties() {
            return new TeamProperties(20);
        }

        @Bean
        TeamService teamService(
                TeamMembershipRepository teamMembershipRepository,
                ChannelAuthorizationPort channelAuthorizationPort,
                ChannelAutoSyncService channelAutoSyncService,
                ChannelRepository channelRepository,
                TeamProperties teamProperties) {
            return new TeamService(
                    teamMembershipRepository,
                    channelAuthorizationPort,
                    channelAutoSyncService,
                    channelRepository,
                    teamProperties);
        }

        @Bean
        PricingRuleService pricingRuleService(
                JooqPricingRuleRepository pricingRuleRepository,
                ChannelAuthorizationPort channelAuthorizationPort,
                ChannelAutoSyncService channelAutoSyncService) {
            return new PricingRuleService(
                    pricingRuleRepository,
                    channelAuthorizationPort,
                    channelAutoSyncService);
        }

        @Bean
        ChannelSearchCriteriaConverter channelSearchCriteriaConverter() {
            return new ChannelSearchCriteriaConverter();
        }

        @Bean
        ChannelController channelController(
                ChannelRegistrationService channelRegistrationService,
                ChannelService channelService,
                ChannelSearchCriteriaConverter converter) {
            return new ChannelController(
                    channelRegistrationService,
                    channelService,
                    converter);
        }

        @Bean
        TeamController teamController(TeamService teamService) {
            return new TeamController(teamService);
        }

        @Bean
        PricingRuleController pricingRuleController(
                PricingRuleService pricingRuleService) {
            return new PricingRuleController(pricingRuleService);
        }
    }
}


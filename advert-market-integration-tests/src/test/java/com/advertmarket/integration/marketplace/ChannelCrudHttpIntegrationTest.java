package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.PricingRulePostTypes.PRICING_RULE_POST_TYPES;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.integration.marketplace.config.MarketplaceTestConfig;
import com.advertmarket.integration.support.ContainerProperties;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.marketplace.api.dto.ChannelListItem;
import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import com.advertmarket.marketplace.api.dto.ChannelSort;
import com.advertmarket.marketplace.api.dto.ChannelSyncResult;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.port.ChannelAutoSyncPort;
import com.advertmarket.marketplace.api.port.CategoryRepository;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.api.port.ChannelSearchPort;
import com.advertmarket.marketplace.channel.adapter.ChannelAuthorizationAdapter;
import com.advertmarket.marketplace.channel.mapper.CategoryDtoMapper;
import com.advertmarket.marketplace.channel.mapper.ChannelRecordMapper;
import com.advertmarket.marketplace.channel.repository.JooqCategoryRepository;
import com.advertmarket.marketplace.channel.repository.JooqChannelRepository;
import com.advertmarket.marketplace.channel.service.ChannelRegistrationService;
import com.advertmarket.marketplace.channel.service.ChannelService;
import com.advertmarket.marketplace.channel.web.ChannelController;
import com.advertmarket.marketplace.channel.web.ChannelSearchCriteriaConverter;
import com.advertmarket.marketplace.pricing.mapper.PricingRuleRecordMapper;
import com.advertmarket.marketplace.pricing.repository.JooqPricingRuleRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * HTTP-level integration tests for channel CRUD endpoints
 * (search, detail, update, deactivate).
 */
@SpringBootTest(
        classes = ChannelCrudHttpIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DisplayName("Channel CRUD HTTP — end-to-end integration")
class ChannelCrudHttpIntegrationTest {

    private static final long OWNER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long MANAGER_WITH_RIGHTS_ID = 3L;
    private static final long MANAGER_WITHOUT_RIGHTS_ID = 4L;
    private static final long CHANNEL_ID = -100L;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        ContainerProperties.registerAll(registry);
    }

    @BeforeAll
    static void initDatabase() throws Exception {
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
    private ChannelSearchPort channelSearchPort;

    @Autowired
    private ChannelAutoSyncPort channelAutoSyncPort;

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        reset(channelAutoSyncPort);
        when(channelAutoSyncPort.syncFromTelegram(anyLong()))
                .thenReturn(new ChannelSyncResult(false, null, OWNER_ID));
        dsl.deleteFrom(PRICING_RULE_POST_TYPES).execute();
        dsl.deleteFrom(CHANNEL_PRICING_RULES).execute();
        dsl.deleteFrom(CHANNEL_CATEGORIES).execute();
        dsl.deleteFrom(CHANNEL_MEMBERSHIPS).execute();
        dsl.deleteFrom(CHANNELS).execute();
        dsl.deleteFrom(USERS).execute();
        TestDataFactory.upsertUser(dsl, OWNER_ID);
        TestDataFactory.upsertUser(dsl, OTHER_USER_ID);
        TestDataFactory.upsertUser(dsl, MANAGER_WITH_RIGHTS_ID);
        TestDataFactory.upsertUser(dsl, MANAGER_WITHOUT_RIGHTS_ID);
    }

    @Test
    @DisplayName("GET /api/v1/channels returns search results")
    void searchReturnsResults() {
        when(channelSearchPort.search(any()))
                .thenReturn(CursorPage.empty());

        webClient.get()
                .uri("/api/v1/channels?limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items").isArray()
                .jsonPath("$.nextCursor").doesNotExist();
    }

    @Test
    @DisplayName("GET /api/v1/channels maps legacy aliases")
    void searchMapsLegacyAliases() {
        when(channelSearchPort.search(any()))
                .thenReturn(CursorPage.empty());
        clearInvocations(channelSearchPort);

        webClient.get()
                .uri("/api/v1/channels?q=ton&minSubs=100&maxSubs=200&sort=er&limit=5")
                .exchange()
                .expectStatus().isOk();

        var captor = ArgumentCaptor.forClass(ChannelSearchCriteria.class);
        verify(channelSearchPort).search(captor.capture());
        ChannelSearchCriteria criteria = captor.getValue();
        assertThat(criteria.query()).isEqualTo("ton");
        assertThat(criteria.minSubscribers()).isEqualTo(100);
        assertThat(criteria.maxSubscribers()).isEqualTo(200);
        assertThat(criteria.sort()).isEqualTo(ChannelSort.ENGAGEMENT_DESC);
        assertThat(criteria.limit()).isEqualTo(5);
    }

    @Test
    @DisplayName("GET /api/v1/channels/count returns numeric count")
    void countReturnsNumber() {
        when(channelSearchPort.count(any())).thenReturn(9L);
        clearInvocations(channelSearchPort);

        webClient.get()
                .uri("/api/v1/channels/count?q=crypto&minSubs=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isEqualTo(9);

        var captor = ArgumentCaptor.forClass(ChannelSearchCriteria.class);
        verify(channelSearchPort).count(captor.capture());
        ChannelSearchCriteria criteria = captor.getValue();
        assertThat(criteria.query()).isEqualTo("crypto");
        assertThat(criteria.minSubscribers()).isEqualTo(10);
    }

    @Test
    @DisplayName("GET /api/v1/channels/{id} returns 200 with detail")
    void getDetailReturns200() {
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);
        TestDataFactory.insertPricingRule(dsl, CHANNEL_ID, "Repost", "REPOST", 1_000_000L, 1);

        webClient.get()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(CHANNEL_ID)
                .jsonPath("$.title").isEqualTo("Test Channel")
                .jsonPath("$.pricingRules").isArray()
                .jsonPath("$.pricingRules.length()").isEqualTo(1);
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id} persists owner note and GET detail returns rules.customRules")
    void putWithCustomRulesPersistsAndReturnsInDetail() {
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);

        webClient.put()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "description": "Rules updated",
                          "customRules": "No gambling ads"
                        }
                        """)
                .exchange()
                .expectStatus().isOk();

        webClient.get()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.rules.customRules")
                .isEqualTo("No gambling ads");
    }

    @Test
    @DisplayName("GET /api/v1/channels/{id} returns 404 for non-existent")
    void getDetailReturns404() {
        webClient.get()
                .uri("/api/v1/channels/{id}", 999)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error_code").isEqualTo("CHANNEL_NOT_FOUND");
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id} by owner returns 200")
    void updateByOwnerReturns200() {
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);

        webClient.put()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelUpdateRequest(
                        "New description", List.of("crypto"),
                        5_000_000L, null, null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.description").isEqualTo("New description")
                .jsonPath("$.categories[0]").isEqualTo("crypto");
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id} by manager with manage_listings returns 200")
    void updateByManagerWithManageListingsReturns200() {
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);
        TestDataFactory.insertManagerMembership(
                dsl,
                CHANNEL_ID,
                MANAGER_WITH_RIGHTS_ID,
                "{\"manage_listings\":true}");

        webClient.put()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(MANAGER_WITH_RIGHTS_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelUpdateRequest(
                        "Updated by manager", null,
                        7_000_000L, null, null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.description").isEqualTo("Updated by manager")
                .jsonPath("$.pricePerPostNano").isEqualTo(7_000_000);
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id} by manager without manage_listings returns 403")
    void updateByManagerWithoutManageListingsReturns403() {
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);
        TestDataFactory.insertManagerMembership(
                dsl,
                CHANNEL_ID,
                MANAGER_WITHOUT_RIGHTS_ID,
                "{\"moderate\":true}");

        webClient.put()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(MANAGER_WITHOUT_RIGHTS_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelUpdateRequest(
                        "Denied", null, null, null, null, null))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code").isEqualTo("CHANNEL_NOT_OWNED");
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id} by non-owner returns 403")
    void updateByNonOwnerReturns403() {
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);

        webClient.put()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OTHER_USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelUpdateRequest(
                        "Hacked", null, null, null, null, null))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code").isEqualTo("CHANNEL_NOT_OWNED");
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id} without auth returns 403")
    void updateWithoutAuthReturns403() {
        webClient.put()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelUpdateRequest(
                        "desc", null, null, null, null, null))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("DELETE /api/v1/channels/{id} by owner returns 204")
    void deactivateByOwnerReturns204() {
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);

        webClient.delete()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .exchange()
                .expectStatus().isNoContent();

        var record = dsl.selectFrom(CHANNELS)
                .where(CHANNELS.ID.eq(CHANNEL_ID))
                .fetchOne();
        assertThat(record).isNotNull();
        assertThat(record.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("DELETE /api/v1/channels/{id} by manager with manage_listings returns 204")
    void deactivateByManagerWithManageListingsReturns204() {
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);
        TestDataFactory.insertManagerMembership(
                dsl,
                CHANNEL_ID,
                MANAGER_WITH_RIGHTS_ID,
                "{\"manage_listings\":true}");

        webClient.delete()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(MANAGER_WITH_RIGHTS_ID)))
                .exchange()
                .expectStatus().isNoContent();

        var record = dsl.selectFrom(CHANNELS)
                .where(CHANNELS.ID.eq(CHANNEL_ID))
                .fetchOne();
        assertThat(record).isNotNull();
        assertThat(record.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("DELETE /api/v1/channels/{id} by non-owner returns 403")
    void deactivateByNonOwnerReturns403() {
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);

        webClient.delete()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OTHER_USER_ID)))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code").isEqualTo("CHANNEL_NOT_OWNED");
    }

    @Test
    @DisplayName("DELETE /api/v1/channels/{id} without auth returns 403")
    void deactivateWithoutAuthReturns403() {
        webClient.delete()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("DELETE /api/v1/channels/{id} non-existent returns 403 (authz before existence)")
    void deactivateNonExistentReturns403() {
        webClient.delete()
                .uri("/api/v1/channels/{id}", 999)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code").isEqualTo("CHANNEL_NOT_OWNED");
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id} returns 503 when live sync fails")
    void updateReturns503WhenLiveSyncFails() {
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);
        when(channelAutoSyncPort.syncFromTelegram(CHANNEL_ID))
                .thenThrow(new DomainException(
                        ErrorCodes.SERVICE_UNAVAILABLE,
                        "telegram unavailable"));

        webClient.put()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelUpdateRequest(
                        "desc", null, null, null, null, null))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("SERVICE_UNAVAILABLE");
    }

    // --- helpers ---

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
        ChannelSearchPort channelSearchPort() {
            return mock(ChannelSearchPort.class);
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
                PricingRuleRecordMapper pricingRuleMapper) {
            return new JooqPricingRuleRepository(dsl, pricingRuleMapper);
        }

        @Bean
        ChannelRepository channelRepository(
                DSLContext dsl,
                ChannelRecordMapper channelMapper,
                CategoryRepository categoryRepo,
                JooqPricingRuleRepository pricingRuleRepo) {
            return new JooqChannelRepository(
                    dsl, channelMapper,
                    categoryRepo, pricingRuleRepo);
        }

        @Bean
        ChannelAuthorizationPort channelAuthorizationPort(DSLContext dsl) {
            return new ChannelAuthorizationAdapter(dsl);
        }

        @Bean
        ChannelService channelService(
                ChannelSearchPort searchPort,
                ChannelRepository repo,
                ChannelAuthorizationPort authPort,
                ChannelAutoSyncPort autoSyncPort) {
            return new ChannelService(
                    searchPort, repo, authPort, autoSyncPort);
        }

        @Bean
        ChannelAutoSyncPort channelAutoSyncPort() {
            return mock(ChannelAutoSyncPort.class);
        }

        @Bean
        ChannelRegistrationService channelRegistrationService() {
            return mock(ChannelRegistrationService.class);
        }

        @Bean
        ChannelSearchCriteriaConverter channelSearchCriteriaConverter() {
            return new ChannelSearchCriteriaConverter();
        }

        @Bean
        ChannelController channelController(
                ChannelRegistrationService registrationService,
                ChannelService channelService,
                ChannelSearchCriteriaConverter converter) {
            return new ChannelController(
                    registrationService, channelService, converter);
        }
    }
}

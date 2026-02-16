package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.PricingRulePostTypes.PRICING_RULE_POST_TYPES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.integration.marketplace.config.MarketplaceTestConfig;
import com.advertmarket.integration.support.ContainerProperties;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.marketplace.api.dto.ChannelSyncResult;
import com.advertmarket.marketplace.api.dto.PricingRuleCreateRequest;
import com.advertmarket.marketplace.api.dto.PricingRuleDto;
import com.advertmarket.marketplace.api.dto.PricingRuleUpdateRequest;
import com.advertmarket.marketplace.api.model.PostType;
import com.advertmarket.marketplace.api.port.ChannelAutoSyncPort;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.port.PricingRuleRepository;
import com.advertmarket.marketplace.channel.adapter.ChannelAuthorizationAdapter;
import com.advertmarket.marketplace.pricing.mapper.PricingRuleRecordMapper;
import com.advertmarket.marketplace.pricing.repository.JooqPricingRuleRepository;
import com.advertmarket.marketplace.pricing.service.PricingRuleService;
import com.advertmarket.marketplace.pricing.web.PricingRuleController;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.model.UserId;
import java.util.Set;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * HTTP-level integration tests for pricing rule CRUD endpoints.
 */
@SpringBootTest(
        classes = PricingRuleHttpIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DisplayName("PricingRule HTTP — end-to-end integration")
class PricingRuleHttpIntegrationTest {

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
    private ChannelAutoSyncPort channelAutoSyncPort;

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        reset(channelAutoSyncPort);
        when(channelAutoSyncPort.syncFromTelegram(anyLong()))
                .thenReturn(new ChannelSyncResult(false, null, OWNER_ID));
        DatabaseSupport.cleanAllTables(dsl);
        TestDataFactory.upsertUser(dsl, OWNER_ID);
        TestDataFactory.upsertUser(dsl, OTHER_USER_ID);
        TestDataFactory.upsertUser(dsl, MANAGER_WITH_RIGHTS_ID);
        TestDataFactory.upsertUser(dsl, MANAGER_WITHOUT_RIGHTS_ID);
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/channels/{id}/pricing returns empty for new channel")
    void listRulesReturnsEmptyForNewChannel() {
        webClient.get()
                .uri("/api/v1/channels/{id}/pricing", CHANNEL_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("POST /api/v1/channels/{id}/pricing by owner returns 201")
    void createRuleByOwnerReturns201() {
        PricingRuleDto body = webClient.post()
                .uri("/api/v1/channels/{id}/pricing", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PricingRuleCreateRequest(
                        "Repost", "Repost description",
                        Set.of(PostType.REPOST), 1_000_000L, 1))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PricingRuleDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.name()).isEqualTo("Repost");
        assertThat(body.postTypes()).containsExactly(PostType.REPOST);
        assertThat(body.priceNano()).isEqualTo(1_000_000L);
        assertThat(body.channelId()).isEqualTo(CHANNEL_ID);
        assertThat(body.isActive()).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/channels/{id}/pricing by manager with manage_listings returns 201")
    void createRuleByManagerWithManageListingsReturns201() {
        TestDataFactory.insertManagerMembership(
                dsl,
                CHANNEL_ID,
                MANAGER_WITH_RIGHTS_ID,
                "{\"manage_listings\":true}");

        webClient.post()
                .uri("/api/v1/channels/{id}/pricing", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(MANAGER_WITH_RIGHTS_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PricingRuleCreateRequest(
                        "Manager Rule", "Managed listing rule",
                        Set.of(PostType.NATIVE), 3_500_000L, 2))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Manager Rule")
                .jsonPath("$.postTypes[0]").isEqualTo("NATIVE");
    }

    @Test
    @DisplayName("POST /api/v1/channels/{id}/pricing by manager without manage_listings returns 403")
    void createRuleByManagerWithoutManageListingsReturns403() {
        TestDataFactory.insertManagerMembership(
                dsl,
                CHANNEL_ID,
                MANAGER_WITHOUT_RIGHTS_ID,
                "{\"moderate\":true}");

        webClient.post()
                .uri("/api/v1/channels/{id}/pricing", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(MANAGER_WITHOUT_RIGHTS_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PricingRuleCreateRequest(
                        "Denied Rule", null, Set.of(PostType.REPOST), 1_000_000L, 1))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code").isEqualTo("CHANNEL_NOT_OWNED");
    }

    @Test
    @DisplayName("POST /api/v1/channels/{id}/pricing by non-owner returns 403")
    void createRuleByNonOwnerReturns403() {
        webClient.post()
                .uri("/api/v1/channels/{id}/pricing", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OTHER_USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PricingRuleCreateRequest(
                        "Repost", null, Set.of(PostType.REPOST), 1_000_000L, 1))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code").isEqualTo("CHANNEL_NOT_OWNED");
    }

    @Test
    @DisplayName("POST /api/v1/channels/{id}/pricing with blank name returns 400")
    void createRuleWithInvalidDataReturns400() {
        webClient.post()
                .uri("/api/v1/channels/{id}/pricing", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"\",\"postTypes\":[\"REPOST\"],"
                        + "\"priceNano\":1000000,\"sortOrder\":1}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error_code").isNotEmpty();
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id}/pricing/{ruleId} by owner returns 200")
    void updateRuleByOwnerReturns200() {
        long ruleId = createTestRule();

        PricingRuleDto body = webClient.put()
                .uri("/api/v1/channels/{channelId}/pricing/{ruleId}",
                        CHANNEL_ID, ruleId)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PricingRuleUpdateRequest(
                        "Updated Name", null, null, 2_000_000L, null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PricingRuleDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.name()).isEqualTo("Updated Name");
        assertThat(body.priceNano()).isEqualTo(2_000_000L);
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id}/pricing/{ruleId} by manager with manage_listings returns 200")
    void updateRuleByManagerWithManageListingsReturns200() {
        TestDataFactory.insertManagerMembership(
                dsl,
                CHANNEL_ID,
                MANAGER_WITH_RIGHTS_ID,
                "{\"manage_listings\":true}");
        long ruleId = createTestRule();

        webClient.put()
                .uri("/api/v1/channels/{channelId}/pricing/{ruleId}",
                        CHANNEL_ID, ruleId)
                .headers(h -> h.setBearerAuth(jwt(MANAGER_WITH_RIGHTS_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PricingRuleUpdateRequest(
                        "Manager Updated Rule", null, null, 2_500_000L, null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Manager Updated Rule")
                .jsonPath("$.priceNano").isEqualTo(2_500_000);
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id}/pricing/{ruleId} non-existent returns 404")
    void updateNonExistentRuleReturns404() {
        webClient.put()
                .uri("/api/v1/channels/{channelId}/pricing/{ruleId}",
                        CHANNEL_ID, 999)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PricingRuleUpdateRequest(
                        "Name", null, null, null, null, null))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("PRICING_RULE_NOT_FOUND");
    }

    @Test
    @DisplayName("DELETE /api/v1/channels/{id}/pricing/{ruleId} by owner returns 204")
    void deleteRuleByOwnerReturns204() {
        long ruleId = createTestRule();

        webClient.delete()
                .uri("/api/v1/channels/{channelId}/pricing/{ruleId}",
                        CHANNEL_ID, ruleId)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("DELETE /api/v1/channels/{id}/pricing/{ruleId} by manager with manage_listings returns 204")
    void deleteRuleByManagerWithManageListingsReturns204() {
        TestDataFactory.insertManagerMembership(
                dsl,
                CHANNEL_ID,
                MANAGER_WITH_RIGHTS_ID,
                "{\"manage_listings\":true}");
        long ruleId = createTestRule();

        webClient.delete()
                .uri("/api/v1/channels/{channelId}/pricing/{ruleId}",
                        CHANNEL_ID, ruleId)
                .headers(h -> h.setBearerAuth(jwt(MANAGER_WITH_RIGHTS_ID)))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("DELETE /api/v1/channels/{id}/pricing/{ruleId} non-existent returns 404")
    void deleteNonExistentRuleReturns404() {
        webClient.delete()
                .uri("/api/v1/channels/{channelId}/pricing/{ruleId}",
                        CHANNEL_ID, 999)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("PRICING_RULE_NOT_FOUND");
    }

    @Test
    @DisplayName("GET /api/v1/channels/{id}/pricing excludes deleted rules")
    void listRulesExcludesDeleted() {
        long ruleId = createTestRule();

        webClient.delete()
                .uri("/api/v1/channels/{channelId}/pricing/{ruleId}",
                        CHANNEL_ID, ruleId)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .exchange()
                .expectStatus().isNoContent();

        webClient.get()
                .uri("/api/v1/channels/{id}/pricing", CHANNEL_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("POST /api/v1/channels/{id}/pricing returns 503 when live sync fails")
    void createRuleReturns503WhenLiveSyncFails() {
        when(channelAutoSyncPort.syncFromTelegram(CHANNEL_ID))
                .thenThrow(new DomainException(
                        ErrorCodes.SERVICE_UNAVAILABLE,
                        "telegram unavailable"));

        webClient.post()
                .uri("/api/v1/channels/{id}/pricing", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PricingRuleCreateRequest(
                        "Repost", null, Set.of(PostType.REPOST), 1_000_000L, 1))
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

    private long createTestRule() {
        long ruleId = dsl.insertInto(CHANNEL_PRICING_RULES)
                .set(CHANNEL_PRICING_RULES.CHANNEL_ID, CHANNEL_ID)
                .set(CHANNEL_PRICING_RULES.NAME, "Test Rule")
                .set(CHANNEL_PRICING_RULES.PRICE_NANO, 1_000_000L)
                .set(CHANNEL_PRICING_RULES.SORT_ORDER, 1)
                .returning(CHANNEL_PRICING_RULES.ID)
                .fetchSingle()
                .getId();
        dsl.insertInto(PRICING_RULE_POST_TYPES)
                .set(PRICING_RULE_POST_TYPES.PRICING_RULE_ID, ruleId)
                .set(PRICING_RULE_POST_TYPES.POST_TYPE, "REPOST")
                .execute();
        return ruleId;
    }

    @Configuration
    @EnableAutoConfiguration
    @Import(MarketplaceTestConfig.class)
    @ComponentScan(basePackages = {
            "com.advertmarket.marketplace.pricing.mapper"
    })
    static class TestConfig {

        @Bean
        ChannelAuthorizationPort channelAuthorizationPort(DSLContext dsl) {
            return new ChannelAuthorizationAdapter(dsl);
        }

        @Bean
        PricingRuleRepository pricingRuleRepository(
                DSLContext dsl,
                PricingRuleRecordMapper mapper) {
            return new JooqPricingRuleRepository(dsl, mapper);
        }

        @Bean
        PricingRuleService pricingRuleService(
                PricingRuleRepository repo,
                ChannelAuthorizationPort authPort,
                ChannelAutoSyncPort autoSyncPort) {
            return new PricingRuleService(repo, authPort, autoSyncPort);
        }

        @Bean
        ChannelAutoSyncPort channelAutoSyncPort() {
            return mock(ChannelAutoSyncPort.class);
        }

        @Bean
        PricingRuleController pricingRuleController(
                PricingRuleService pricingRuleService) {
            return new PricingRuleController(pricingRuleService);
        }
    }
}

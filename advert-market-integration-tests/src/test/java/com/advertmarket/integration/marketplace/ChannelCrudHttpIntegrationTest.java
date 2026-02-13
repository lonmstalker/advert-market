package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.PricingRulePostTypes.PRICING_RULE_POST_TYPES;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.advertmarket.integration.marketplace.config.MarketplaceTestConfig;
import com.advertmarket.marketplace.api.dto.ChannelListItem;
import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.port.CategoryRepository;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.api.port.ChannelSearchPort;
import com.advertmarket.marketplace.channel.adapter.ChannelAuthorizationAdapter;
import com.advertmarket.marketplace.channel.mapper.ChannelRecordMapper;
import com.advertmarket.marketplace.channel.repository.JooqCategoryRepository;
import com.advertmarket.marketplace.channel.repository.JooqChannelRepository;
import com.advertmarket.marketplace.channel.service.ChannelRegistrationService;
import com.advertmarket.marketplace.channel.service.ChannelService;
import com.advertmarket.marketplace.channel.web.ChannelController;
import com.advertmarket.marketplace.pricing.mapper.PricingRuleRecordMapper;
import com.advertmarket.marketplace.pricing.repository.JooqPricingRuleRepository;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.identity.security.JwtTokenProvider;
import java.util.List;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jooq.DSLContext;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * HTTP-level integration tests for channel CRUD endpoints
 * (search, detail, update, deactivate).
 */
@SpringBootTest(
        classes = ChannelCrudHttpIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@DisplayName("Channel CRUD HTTP — end-to-end integration")
class ChannelCrudHttpIntegrationTest {

    private static final long OWNER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long CHANNEL_ID = -100L;

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
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
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
    private ChannelSearchPort channelSearchPort;

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        dsl.deleteFrom(PRICING_RULE_POST_TYPES).execute();
        dsl.deleteFrom(CHANNEL_PRICING_RULES).execute();
        dsl.deleteFrom(CHANNEL_CATEGORIES).execute();
        dsl.deleteFrom(CHANNEL_MEMBERSHIPS).execute();
        dsl.deleteFrom(CHANNELS).execute();
        dsl.deleteFrom(USERS).execute();
        upsertUser(OWNER_ID);
        upsertUser(OTHER_USER_ID);
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
    @DisplayName("GET /api/v1/channels/{id} returns 200 with detail")
    void getDetailReturns200() {
        insertChannelWithOwner(CHANNEL_ID, OWNER_ID);
        insertPricingRule(CHANNEL_ID, "Repost", "REPOST", 1_000_000L);

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
        insertChannelWithOwner(CHANNEL_ID, OWNER_ID);

        webClient.put()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelUpdateRequest(
                        "New description", List.of("crypto"),
                        5_000_000L, null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.description").isEqualTo("New description")
                .jsonPath("$.categories[0]").isEqualTo("crypto");
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id} by non-owner returns 403")
    void updateByNonOwnerReturns403() {
        insertChannelWithOwner(CHANNEL_ID, OWNER_ID);

        webClient.put()
                .uri("/api/v1/channels/{id}", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OTHER_USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChannelUpdateRequest(
                        "Hacked", null, null, null, null))
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
                        "desc", null, null, null, null))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("DELETE /api/v1/channels/{id} by owner returns 204")
    void deactivateByOwnerReturns204() {
        insertChannelWithOwner(CHANNEL_ID, OWNER_ID);

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
    @DisplayName("DELETE /api/v1/channels/{id} by non-owner returns 403")
    void deactivateByNonOwnerReturns403() {
        insertChannelWithOwner(CHANNEL_ID, OWNER_ID);

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

    // --- helpers ---

    private String jwt(long userId) {
        return jwtTokenProvider.generateToken(
                new UserId(userId), false);
    }

    private void upsertUser(long userId) {
        dsl.insertInto(USERS)
                .set(USERS.ID, userId)
                .set(USERS.FIRST_NAME, "U" + userId)
                .set(USERS.LANGUAGE_CODE, "en")
                .onConflictDoNothing()
                .execute();
    }

    private void insertChannelWithOwner(long channelId, long ownerId) {
        dsl.insertInto(CHANNELS)
                .set(CHANNELS.ID, channelId)
                .set(CHANNELS.TITLE, "Test Channel")
                .set(CHANNELS.SUBSCRIBER_COUNT, 5000)
                .set(CHANNELS.OWNER_ID, ownerId)
                .execute();
        dsl.insertInto(CHANNEL_MEMBERSHIPS)
                .set(CHANNEL_MEMBERSHIPS.CHANNEL_ID, channelId)
                .set(CHANNEL_MEMBERSHIPS.USER_ID, ownerId)
                .set(CHANNEL_MEMBERSHIPS.ROLE, "OWNER")
                .execute();
    }

    private void insertPricingRule(long channelId, String name,
                                   String postType, long priceNano) {
        long ruleId = dsl.insertInto(CHANNEL_PRICING_RULES)
                .set(CHANNEL_PRICING_RULES.CHANNEL_ID, channelId)
                .set(CHANNEL_PRICING_RULES.NAME, name)
                .set(CHANNEL_PRICING_RULES.PRICE_NANO, priceNano)
                .returning(CHANNEL_PRICING_RULES.ID)
                .fetchSingle()
                .getId();
        dsl.insertInto(PRICING_RULE_POST_TYPES)
                .set(PRICING_RULE_POST_TYPES.PRICING_RULE_ID, ruleId)
                .set(PRICING_RULE_POST_TYPES.POST_TYPE, postType)
                .execute();
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
                DSLContext dsl, JsonFacade jsonFacade) {
            return new JooqCategoryRepository(dsl, jsonFacade);
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
                PricingRuleRecordMapper pricingRuleMapper,
                CategoryRepository categoryRepo,
                JooqPricingRuleRepository pricingRuleRepo) {
            return new JooqChannelRepository(
                    dsl, channelMapper, pricingRuleMapper,
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
                ChannelAuthorizationPort authPort) {
            return new ChannelService(searchPort, repo, authPort);
        }

        @Bean
        ChannelRegistrationService channelRegistrationService() {
            return mock(ChannelRegistrationService.class);
        }

        @Bean
        ChannelController channelController(
                ChannelRegistrationService registrationService,
                ChannelService channelService) {
            return new ChannelController(
                    registrationService, channelService);
        }
    }
}

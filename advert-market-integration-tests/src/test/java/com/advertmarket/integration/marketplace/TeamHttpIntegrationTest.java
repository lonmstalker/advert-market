package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.integration.marketplace.config.MarketplaceTestConfig;
import com.advertmarket.marketplace.api.dto.TeamInviteRequest;
import com.advertmarket.marketplace.api.dto.TeamMemberDto;
import com.advertmarket.marketplace.api.dto.TeamUpdateRightsRequest;
import com.advertmarket.marketplace.api.model.ChannelRight;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.dto.NewChannel;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.api.port.TeamMembershipRepository;
import com.advertmarket.marketplace.channel.adapter.ChannelAuthorizationAdapter;
import com.advertmarket.marketplace.team.repository.JooqTeamMembershipRepository;
import com.advertmarket.marketplace.team.service.TeamService;
import com.advertmarket.marketplace.team.web.TeamController;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.identity.security.JwtTokenProvider;
import java.util.Optional;
import java.util.Set;
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
 * HTTP-level integration tests for channel team management endpoints.
 */
@SpringBootTest(
        classes = TeamHttpIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@DisplayName("Team HTTP — end-to-end integration")
class TeamHttpIntegrationTest {

    private static final long OWNER_ID = 1L;
    private static final long MANAGER_ID = 2L;
    private static final long OTHER_USER_ID = 3L;
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

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        dsl.deleteFrom(CHANNEL_MEMBERSHIPS).execute();
        dsl.deleteFrom(CHANNELS).execute();
        dsl.deleteFrom(USERS).execute();
        upsertUser(OWNER_ID, "owner", "Owner");
        upsertUser(MANAGER_ID, "manager", "Manager");
        upsertUser(OTHER_USER_ID, "other", "Other");
        insertChannelWithOwner(CHANNEL_ID, OWNER_ID);
    }

    // --- LIST ---

    @Test
    @DisplayName("GET /api/v1/channels/{id}/team by owner returns team members")
    void listByOwnerReturnsMember() {
        webClient.get()
                .uri("/api/v1/channels/{id}/team", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].role").isEqualTo("OWNER");
    }

    @Test
    @DisplayName("GET /api/v1/channels/{id}/team by non-member returns 403")
    void listByNonMemberReturns403() {
        webClient.get()
                .uri("/api/v1/channels/{id}/team", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OTHER_USER_ID)))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code").isEqualTo("CHANNEL_NOT_OWNED");
    }

    // --- INVITE ---

    @Test
    @DisplayName("POST /api/v1/channels/{id}/team by owner returns 201")
    void inviteByOwnerReturns201() {
        TeamMemberDto body = webClient.post()
                .uri("/api/v1/channels/{id}/team", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TeamInviteRequest(MANAGER_ID,
                        Set.of(ChannelRight.MODERATE, ChannelRight.PUBLISH)))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TeamMemberDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.userId()).isEqualTo(MANAGER_ID);
        assertThat(body.role().name()).isEqualTo("MANAGER");
        assertThat(body.rights()).containsExactlyInAnyOrder(
                ChannelRight.MODERATE, ChannelRight.PUBLISH);
        assertThat(body.invitedBy()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("POST /api/v1/channels/{id}/team by non-owner returns 403")
    void inviteByNonOwnerReturns403() {
        webClient.post()
                .uri("/api/v1/channels/{id}/team", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(MANAGER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TeamInviteRequest(OTHER_USER_ID,
                        Set.of(ChannelRight.MODERATE)))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code").isEqualTo("CHANNEL_NOT_OWNED");
    }

    @Test
    @DisplayName("POST /api/v1/channels/{id}/team duplicate invite returns 409")
    void duplicateInviteReturns409() {
        inviteMember(MANAGER_ID, Set.of(ChannelRight.MODERATE));

        webClient.post()
                .uri("/api/v1/channels/{id}/team", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TeamInviteRequest(MANAGER_ID,
                        Set.of(ChannelRight.PUBLISH)))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("TEAM_MEMBER_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("POST /api/v1/channels/{id}/team non-existent user returns 404")
    void inviteNonExistentUserReturns404() {
        webClient.post()
                .uri("/api/v1/channels/{id}/team", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TeamInviteRequest(999L,
                        Set.of(ChannelRight.MODERATE)))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error_code").isEqualTo("USER_NOT_FOUND");
    }

    // --- UPDATE RIGHTS ---

    @Test
    @DisplayName("PUT /api/v1/channels/{id}/team/{userId} by owner returns 200")
    void updateRightsByOwnerReturns200() {
        inviteMember(MANAGER_ID, Set.of(ChannelRight.MODERATE));

        TeamMemberDto body = webClient.put()
                .uri("/api/v1/channels/{channelId}/team/{userId}",
                        CHANNEL_ID, MANAGER_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TeamUpdateRightsRequest(
                        Set.of(ChannelRight.PUBLISH, ChannelRight.VIEW_STATS)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TeamMemberDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.rights()).containsExactlyInAnyOrder(
                ChannelRight.PUBLISH, ChannelRight.VIEW_STATS);
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id}/team/{ownerId} returns 403 (owner protected)")
    void updateOwnerRightsReturns403() {
        webClient.put()
                .uri("/api/v1/channels/{channelId}/team/{userId}",
                        CHANNEL_ID, OWNER_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TeamUpdateRightsRequest(
                        Set.of(ChannelRight.PUBLISH)))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("TEAM_OWNER_PROTECTED");
    }

    @Test
    @DisplayName("PUT /api/v1/channels/{id}/team/{userId} non-member returns 404")
    void updateNonMemberReturns404() {
        webClient.put()
                .uri("/api/v1/channels/{channelId}/team/{userId}",
                        CHANNEL_ID, OTHER_USER_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TeamUpdateRightsRequest(
                        Set.of(ChannelRight.PUBLISH)))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("TEAM_MEMBER_NOT_FOUND");
    }

    // --- REMOVE ---

    @Test
    @DisplayName("DELETE /api/v1/channels/{id}/team/{userId} by owner returns 204")
    void removeByOwnerReturns204() {
        inviteMember(MANAGER_ID, Set.of(ChannelRight.MODERATE));

        webClient.delete()
                .uri("/api/v1/channels/{channelId}/team/{userId}",
                        CHANNEL_ID, MANAGER_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("DELETE /api/v1/channels/{id}/team/{userId} self-removal by manager returns 204")
    void selfRemovalByManagerReturns204() {
        inviteMember(MANAGER_ID, Set.of(ChannelRight.MODERATE));

        webClient.delete()
                .uri("/api/v1/channels/{channelId}/team/{userId}",
                        CHANNEL_ID, MANAGER_ID)
                .headers(h -> h.setBearerAuth(jwt(MANAGER_ID)))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("DELETE /api/v1/channels/{id}/team/{ownerId} returns 403 (owner protected)")
    void removeOwnerReturns403() {
        webClient.delete()
                .uri("/api/v1/channels/{channelId}/team/{userId}",
                        CHANNEL_ID, OWNER_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("TEAM_OWNER_PROTECTED");
    }

    @Test
    @DisplayName("CRUD happy path — invite, update rights, list, remove")
    void fullCrudHappyPath() {
        // invite
        webClient.post()
                .uri("/api/v1/channels/{id}/team", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TeamInviteRequest(MANAGER_ID,
                        Set.of(ChannelRight.MODERATE)))
                .exchange()
                .expectStatus().isCreated();

        // update rights
        webClient.put()
                .uri("/api/v1/channels/{channelId}/team/{userId}",
                        CHANNEL_ID, MANAGER_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TeamUpdateRightsRequest(
                        Set.of(ChannelRight.PUBLISH, ChannelRight.VIEW_STATS)))
                .exchange()
                .expectStatus().isOk();

        // list
        webClient.get()
                .uri("/api/v1/channels/{id}/team", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);

        // remove
        webClient.delete()
                .uri("/api/v1/channels/{channelId}/team/{userId}",
                        CHANNEL_ID, MANAGER_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .exchange()
                .expectStatus().isNoContent();

        // list after removal
        webClient.get()
                .uri("/api/v1/channels/{id}/team", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1);
    }

    // --- helpers ---

    private String jwt(long userId) {
        return jwtTokenProvider.generateToken(
                new UserId(userId), false);
    }

    private void inviteMember(long userId, Set<ChannelRight> rights) {
        webClient.post()
                .uri("/api/v1/channels/{id}/team", CHANNEL_ID)
                .headers(h -> h.setBearerAuth(jwt(OWNER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TeamInviteRequest(userId, rights))
                .exchange()
                .expectStatus().isCreated();
    }

    private void upsertUser(long userId, String username, String firstName) {
        dsl.insertInto(USERS)
                .set(USERS.ID, userId)
                .set(USERS.USERNAME, username)
                .set(USERS.FIRST_NAME, firstName)
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

    @Configuration
    @EnableAutoConfiguration
    @Import(MarketplaceTestConfig.class)
    static class TestConfig {

        @Bean
        ChannelAuthorizationPort channelAuthorizationPort(DSLContext dsl) {
            return new ChannelAuthorizationAdapter(dsl);
        }

        @Bean
        TeamMembershipRepository teamMembershipRepository(
                DSLContext dsl, JsonFacade jsonFacade) {
            return new JooqTeamMembershipRepository(dsl, jsonFacade);
        }

        @Bean
        ChannelRepository channelRepository(DSLContext dslContext) {
            return new ChannelRepository() {
                @Override
                public boolean existsByTelegramId(long telegramId) {
                    return dslContext.fetchExists(
                            dslContext.selectOne()
                                    .from(CHANNELS)
                                    .where(CHANNELS.ID.eq(telegramId)));
                }

                @Override
                public ChannelResponse insert(NewChannel newChannel) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Optional<ChannelResponse> findByTelegramId(long telegramId) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Optional<ChannelDetailResponse> findDetailById(long channelId) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Optional<ChannelResponse> update(long channelId, ChannelUpdateRequest request) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean deactivate(long channelId) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Bean
        TeamService teamService(
                TeamMembershipRepository repo,
                ChannelAuthorizationPort authPort,
                ChannelRepository channelRepo) {
            return new TeamService(repo, authPort, channelRepo);
        }

        @Bean
        TeamController teamController(TeamService teamService) {
            return new TeamController(teamService);
        }
    }
}

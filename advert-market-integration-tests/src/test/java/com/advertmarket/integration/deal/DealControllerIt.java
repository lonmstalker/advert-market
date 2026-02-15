package com.advertmarket.integration.deal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.advertmarket.app.outbox.JooqOutboxRepository;
import com.advertmarket.deal.api.dto.DealDetailDto;
import com.advertmarket.deal.api.dto.DealDto;
import com.advertmarket.identity.security.JwtAuthenticationFilter;
import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.integration.marketplace.config.MarketplaceTestConfig;
import com.advertmarket.integration.support.ContainerProperties;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.outbox.OutboxRepository;
import com.advertmarket.shared.pagination.CursorPage;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * HTTP-level integration tests for Deal endpoints.
 *
 * <p>Validates REST API contracts: HTTP codes, JSON format,
 * validation, and authorization.
 */
@SpringBootTest(
        classes = DealControllerIt.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@DisplayName("Deal HTTP — end-to-end integration")
class DealControllerIt {

    private static final long ADVERTISER_ID = 100L;
    private static final long OWNER_ID = 200L;
    private static final long NON_PARTICIPANT_ID = 300L;
    private static final long CHANNEL_ID = -1001234567890L;
    private static final long UNKNOWN_CHANNEL_ID = 999_999L;
    private static final long ONE_TON_NANO = 1_000_000_000L;

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

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        DatabaseSupport.cleanAllTables(dsl);
        TestDataFactory.upsertUser(dsl, ADVERTISER_ID);
        TestDataFactory.upsertUser(dsl, OWNER_ID);
        TestDataFactory.upsertUser(dsl, NON_PARTICIPANT_ID);
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);
    }

    // --- POST /deals ---

    @Test
    @DisplayName("POST /deals with valid body returns 201 and DealDto")
    void createDeal_returns201() {
        String token = advertiserToken();

        var body = webClient.post().uri("/api/v1/deals")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest(CHANNEL_ID, ONE_TON_NANO))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DealDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.channelId()).isEqualTo(CHANNEL_ID);
        assertThat(body.advertiserId()).isEqualTo(ADVERTISER_ID);
        assertThat(body.ownerId()).isEqualTo(OWNER_ID);
        assertThat(body.status().name()).isEqualTo("DRAFT");
        assertThat(body.amountNano()).isEqualTo(ONE_TON_NANO);
    }

    @Test
    @DisplayName("POST /deals with non-existent channel returns 404")
    void createDeal_channelNotFound_returns404() {
        String token = advertiserToken();

        webClient.post().uri("/api/v1/deals")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest(UNKNOWN_CHANNEL_ID, ONE_TON_NANO))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error_code").isEqualTo("CHANNEL_NOT_FOUND");
    }

    @Test
    @DisplayName("POST /deals with zero amount returns 400")
    void createDeal_zeroAmount_returns400() {
        String token = advertiserToken();

        webClient.post().uri("/api/v1/deals")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest(CHANNEL_ID, 0L))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // --- GET /deals/{id} ---

    @Test
    @DisplayName("GET /deals/{id} as participant returns 200 with DealDetailDto")
    void getDealDetail_asParticipant_returns200() {
        String token = advertiserToken();
        var dealId = createDealViaApi(token);

        var body = webClient.get()
                .uri("/api/v1/deals/{id}", dealId)
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(DealDetailDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.id().value()).isEqualTo(dealId);
        assertThat(body.timeline()).isNotNull();
    }

    @Test
    @DisplayName("GET /deals/{id} as non-participant returns 403")
    void getDealDetail_asNonParticipant_returns403() {
        String advertiserToken = advertiserToken();
        var dealId = createDealViaApi(advertiserToken);

        String nonParticipantToken = TestDataFactory.jwt(
                jwtTokenProvider, NON_PARTICIPANT_ID);

        webClient.get()
                .uri("/api/v1/deals/{id}", dealId)
                .headers(h -> h.setBearerAuth(nonParticipantToken))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("DEAL_NOT_PARTICIPANT");
    }

    // --- GET /deals ---

    @Test
    @DisplayName("GET /deals with existing deals returns 200 with items")
    void listDeals_withDeals_returns200() {
        String token = advertiserToken();
        createDealViaApi(token);
        createDealViaApi(token);

        var body = webClient.get().uri("/api/v1/deals")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference
                        <CursorPage<DealDto>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.items()).hasSize(2);
    }

    @Test
    @DisplayName("GET /deals with no deals returns 200 with empty page")
    void listDeals_empty_returns200() {
        String token = TestDataFactory.jwt(
                jwtTokenProvider, NON_PARTICIPANT_ID);

        var body = webClient.get().uri("/api/v1/deals")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference
                        <CursorPage<DealDto>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.items()).isEmpty();
        assertThat(body.nextCursor()).isNull();
    }

    // --- POST /deals/{id}/transition ---

    @Test
    @DisplayName("POST /deals/{id}/transition DRAFT→OFFER_PENDING returns 200")
    void transition_draftToOfferPending_returns200() {
        String token = advertiserToken();
        var dealId = createDealViaApi(token);

        webClient.post()
                .uri("/api/v1/deals/{id}/transition", dealId)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transitionRequest("OFFER_PENDING"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.newStatus").isEqualTo("OFFER_PENDING");
    }

    @Test
    @DisplayName("POST /deals/{id}/transition with invalid transition returns 409")
    void transition_invalid_returns409() {
        String token = advertiserToken();
        var dealId = createDealViaApi(token);

        webClient.post()
                .uri("/api/v1/deals/{id}/transition", dealId)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transitionRequest("FUNDED"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("INVALID_STATE_TRANSITION");
    }

    @Test
    @DisplayName("POST /deals/{id}/transition for non-existent deal returns 404")
    void transition_dealNotFound_returns404() {
        String token = advertiserToken();

        webClient.post()
                .uri("/api/v1/deals/{id}/transition",
                        UUID.randomUUID())
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transitionRequest("OFFER_PENDING"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("DEAL_NOT_FOUND");
    }

    // --- helpers ---

    private String advertiserToken() {
        return TestDataFactory.jwt(jwtTokenProvider, ADVERTISER_ID);
    }

    private UUID createDealViaApi(String token) {
        var body = webClient.post().uri("/api/v1/deals")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest(CHANNEL_ID, ONE_TON_NANO))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DealDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(body).isNotNull();
        return body.id().value();
    }

    private static Map<String, Object> createRequest(
            long channelId, long amountNano) {
        return Map.of(
                "channelId", channelId,
                "amountNano", amountNano);
    }

    private static Map<String, String> transitionRequest(
            String targetStatus) {
        return Map.of("targetStatus", targetStatus);
    }

    /**
     * Minimal Spring config for deal HTTP tests.
     */
    @Configuration
    @EnableAutoConfiguration
    @Import(MarketplaceTestConfig.class)
    @ComponentScan(basePackages = "com.advertmarket.deal")
    static class TestConfig {

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
                            UsernamePasswordAuthenticationFilter.class)
                    .build();
        }

        @Bean
        ChannelRepository channelRepository(DSLContext dsl) {
            var repo = mock(ChannelRepository.class);
            when(repo.findDetailById(anyLong())).thenAnswer(inv -> {
                long id = inv.getArgument(0);
                return dsl.select()
                        .from(com.advertmarket.db.generated.tables
                                .Channels.CHANNELS)
                        .where(com.advertmarket.db.generated.tables
                                .Channels.CHANNELS.ID.eq(id))
                        .fetchOptional(r -> new ChannelDetailResponse(
                                r.get(com.advertmarket.db.generated
                                        .tables.Channels.CHANNELS.ID),
                                r.get(com.advertmarket.db.generated
                                        .tables.Channels.CHANNELS.TITLE),
                                null, null,
                                r.get(com.advertmarket.db.generated
                                        .tables.Channels
                                        .CHANNELS.SUBSCRIBER_COUNT),
                                List.of(), null, true,
                                r.get(com.advertmarket.db.generated
                                        .tables.Channels
                                        .CHANNELS.OWNER_ID),
                                null, 0, null, List.of(),
                                OffsetDateTime.now(),
                                OffsetDateTime.now()));
            });
            return repo;
        }

        @Bean
        OutboxRepository outboxRepository(DSLContext dsl) {
            return new JooqOutboxRepository(dsl);
        }
    }
}

package com.advertmarket.integration.deal;

import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.CreativeTemplates.CREATIVE_TEMPLATES;
import static com.advertmarket.db.generated.tables.DealEvents.DEAL_EVENTS;
import static com.advertmarket.db.generated.tables.Deals.DEALS;
import static com.advertmarket.db.generated.tables.NotificationOutbox.NOTIFICATION_OUTBOX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.advertmarket.app.outbox.JooqOutboxRepository;
import com.advertmarket.deal.api.dto.DealDetailDto;
import com.advertmarket.deal.api.dto.DealDto;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.service.DealService;
import com.advertmarket.financial.api.port.DepositPort;
import com.advertmarket.financial.api.port.EscrowPort;
import com.advertmarket.identity.security.JwtAuthenticationFilter;
import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.integration.marketplace.config.MarketplaceTestConfig;
import com.advertmarket.integration.support.ContainerProperties;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelSyncResult;
import com.advertmarket.marketplace.api.port.ChannelAutoSyncPort;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.api.port.CreativeRepository;
import com.advertmarket.marketplace.channel.adapter.ChannelAuthorizationAdapter;
import com.advertmarket.marketplace.creative.repository.JooqCreativeTemplateRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.event.EventEnvelopeDeserializer;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.outbox.OutboxRepository;
import com.advertmarket.shared.pagination.CursorPage;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
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
import org.jooq.impl.DSL;

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
    private static final long NEW_OWNER_ID = 400L;
    private static final long MANAGER_ID = 500L;
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

    @Autowired
    private DealService dealService;

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
        TestDataFactory.upsertUser(dsl, ADVERTISER_ID);
        TestDataFactory.upsertUser(dsl, OWNER_ID);
        TestDataFactory.upsertUser(dsl, NON_PARTICIPANT_ID);
        TestDataFactory.upsertUser(dsl, MANAGER_ID);
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
    @DisplayName("POST /deals should fallback to cached channel when live sync is rate-limited")
    void createDeal_syncRateLimited_returns201() {
        String token = advertiserToken();
        when(channelAutoSyncPort.syncFromTelegram(CHANNEL_ID))
                .thenThrow(new DomainException(
                        ErrorCodes.RATE_LIMIT_EXCEEDED,
                        "rate limited"));

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
        assertThat(body.channelId()).isEqualTo(CHANNEL_ID);
        assertThat(body.status().name()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("POST /deals with plain text creativeBrief returns 201 and persists wrapped JSON")
    void createDeal_plainTextCreativeBrief_returns201() {
        String token = advertiserToken();

        var body = webClient.post().uri("/api/v1/deals")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "channelId", CHANNEL_ID,
                        "amountNano", ONE_TON_NANO,
                        "creativeBrief", "Need native integration"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DealDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        String storedCreativeBrief = dsl.select(DEALS.CREATIVE_BRIEF)
                .from(DEALS)
                .where(DEALS.ID.eq(body.id().value()))
                .fetchSingle(DEALS.CREATIVE_BRIEF)
                .data();
        assertThat(storedCreativeBrief).contains("Need native integration");
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

    @Test
    @DisplayName("POST /deals with creativeId stores creative snapshot into creative_brief")
    void createDeal_withCreativeId_storesSnapshot() {
        String token = advertiserToken();
        UUID creativeId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        dsl.insertInto(CREATIVE_TEMPLATES)
                .set(CREATIVE_TEMPLATES.ID, creativeId)
                .set(CREATIVE_TEMPLATES.OWNER_USER_ID, ADVERTISER_ID)
                .set(CREATIVE_TEMPLATES.TITLE, "Creative for launch")
                .set(CREATIVE_TEMPLATES.DRAFT, JSONB.valueOf("""
                        {"text":"Install app","entities":[],"media":[],"keyboardRows":[],"disableWebPagePreview":false}
                        """))
                .set(CREATIVE_TEMPLATES.VERSION, 2)
                .set(CREATIVE_TEMPLATES.IS_DELETED, false)
                .set(CREATIVE_TEMPLATES.CREATED_AT, now)
                .set(CREATIVE_TEMPLATES.UPDATED_AT, now)
                .execute();

        var body = webClient.post().uri("/api/v1/deals")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "channelId", CHANNEL_ID,
                        "amountNano", ONE_TON_NANO,
                        "creativeId", creativeId.toString()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DealDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        String storedCreativeBrief = dsl.select(DEALS.CREATIVE_BRIEF)
                .from(DEALS)
                .where(DEALS.ID.eq(body.id().value()))
                .fetchSingle(DEALS.CREATIVE_BRIEF)
                .data();
        assertThat(storedCreativeBrief)
                .contains(creativeId.toString())
                .contains("Creative for launch")
                .contains("Install app");
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

    @Test
    @DisplayName("POST /deals/{id}/transition owner-side returns 503 when live sync fails")
    void transition_ownerSideSyncFailure_returns503() {
        UUID dealId = createDealViaApi(advertiserToken());
        transitionByToken(advertiserToken(), dealId, "OFFER_PENDING");
        when(channelAutoSyncPort.syncFromTelegram(CHANNEL_ID))
                .thenThrow(new DomainException(
                        ErrorCodes.SERVICE_UNAVAILABLE,
                        "telegram unavailable"));

        webClient.post()
                .uri("/api/v1/deals/{id}/transition", dealId)
                .headers(h -> h.setBearerAuth(ownerToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transitionRequest("ACCEPTED"))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("SERVICE_UNAVAILABLE");
    }

    @Test
    @DisplayName("POST /deals/{id}/transition owner-side falls back to cached channel when sync is rate-limited")
    void transition_ownerSideSyncRateLimited_returns200() {
        UUID dealId = createDealViaApi(advertiserToken());
        transitionByToken(advertiserToken(), dealId, "OFFER_PENDING");
        when(channelAutoSyncPort.syncFromTelegram(CHANNEL_ID))
                .thenThrow(new DomainException(
                        ErrorCodes.RATE_LIMIT_EXCEEDED,
                        "rate limited"));

        webClient.post()
                .uri("/api/v1/deals/{id}/transition", dealId)
                .headers(h -> h.setBearerAuth(ownerToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transitionRequest("ACCEPTED"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.newStatus").isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("Full workflow: create deal with creative brief and reach COMPLETED_RELEASED")
    void fullWorkflow_createDealAndCreativeToCompletedReleased() {
        String creativeBrief = """
                {"goal":"Install app","cta":"Open mini app","format":"post"}
                """;

        var createBody = webClient.post().uri("/api/v1/deals")
                .headers(h -> h.setBearerAuth(advertiserToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "channelId", CHANNEL_ID,
                        "amountNano", ONE_TON_NANO,
                        "creativeBrief", creativeBrief))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DealDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(createBody).isNotNull();
        UUID dealId = createBody.id().value();

        String storedCreativeBrief = dsl.select(DEALS.CREATIVE_BRIEF)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchSingle(DEALS.CREATIVE_BRIEF)
                .data();
        assertThat(storedCreativeBrief)
                .contains("Install app")
                .contains("Open mini app")
                .contains("post");

        transitionByToken(advertiserToken(), dealId, "OFFER_PENDING");
        transitionByToken(ownerToken(), dealId, "ACCEPTED");
        transitionAsSystem(dealId, DealStatus.AWAITING_PAYMENT);
        transitionAsSystem(dealId, DealStatus.FUNDED);
        transitionByToken(ownerToken(), dealId, "CREATIVE_SUBMITTED");
        transitionByToken(advertiserToken(), dealId, "CREATIVE_APPROVED");
        transitionByToken(ownerToken(), dealId, "PUBLISHED");
        transitionAsSystem(dealId, DealStatus.DELIVERY_VERIFYING);
        transitionAsSystem(dealId, DealStatus.COMPLETED_RELEASED);

        var detail = webClient.get()
                .uri("/api/v1/deals/{id}", dealId)
                .headers(h -> h.setBearerAuth(advertiserToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(DealDetailDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(detail).isNotNull();
        assertThat(detail.status()).isEqualTo(DealStatus.COMPLETED_RELEASED);
        assertThat(detail.timeline()).hasSize(9);
        assertThat(detail.timeline().getFirst().toStatus())
                .isEqualTo(DealStatus.COMPLETED_RELEASED);

        String finalStatus = dsl.select(DEALS.STATUS)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchSingle(DEALS.STATUS);
        assertThat(finalStatus).isEqualTo(DealStatus.COMPLETED_RELEASED.name());

        int stateChangeEvents = dsl.selectCount()
                .from(DEAL_EVENTS)
                .where(DEAL_EVENTS.DEAL_ID.eq(dealId))
                .and(DEAL_EVENTS.EVENT_TYPE.eq("DEAL_STATE_CHANGED"))
                .fetchOne(0, int.class);
        assertThat(stateChangeEvents).isEqualTo(9);

        int outboxEvents = dsl.selectCount()
                .from(NOTIFICATION_OUTBOX)
                .where(NOTIFICATION_OUTBOX.DEAL_ID.eq(dealId))
                .and(NOTIFICATION_OUTBOX.TOPIC.eq(TopicNames.DEAL_STATE_CHANGED))
                .fetchOne(0, int.class);
        assertThat(outboxEvents).isEqualTo(9);
    }

    @Test
    @DisplayName("Owner transfer reassigns non-terminal deal owner and appends audit event")
    void ownerTransfer_reassignsDealOwner_andAppendsAuditEvent() {
        TestDataFactory.upsertUser(dsl, NEW_OWNER_ID);
        UUID dealId = createDealViaApi(advertiserToken());
        transitionByToken(advertiserToken(), dealId, "OFFER_PENDING");

        dsl.update(CHANNELS)
                .set(CHANNELS.OWNER_ID, NEW_OWNER_ID)
                .where(CHANNELS.ID.eq(CHANNEL_ID))
                .execute();
        dsl.deleteFrom(CHANNEL_MEMBERSHIPS)
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(CHANNEL_ID))
                .execute();
        dsl.insertInto(CHANNEL_MEMBERSHIPS)
                .set(CHANNEL_MEMBERSHIPS.CHANNEL_ID, CHANNEL_ID)
                .set(CHANNEL_MEMBERSHIPS.USER_ID, NEW_OWNER_ID)
                .set(CHANNEL_MEMBERSHIPS.ROLE, "OWNER")
                .execute();

        when(channelAutoSyncPort.syncFromTelegram(CHANNEL_ID))
                .thenReturn(new ChannelSyncResult(
                        true, OWNER_ID, NEW_OWNER_ID));

        webClient.post()
                .uri("/api/v1/deals/{id}/transition", dealId)
                .headers(h -> h.setBearerAuth(newOwnerToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transitionRequest("ACCEPTED"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.newStatus").isEqualTo("ACCEPTED");

        Long dealOwnerId = dsl.select(DEALS.OWNER_ID)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchOne(DEALS.OWNER_ID);
        assertThat(dealOwnerId).isEqualTo(NEW_OWNER_ID);

        int reassignmentEvents = dsl.selectCount()
                .from(DEAL_EVENTS)
                .where(DEAL_EVENTS.DEAL_ID.eq(dealId))
                .and(DEAL_EVENTS.EVENT_TYPE.eq("AUDIT_EVENT"))
                .and(DSL.condition(
                        "payload::text like {0}",
                        "%OWNER_REASSIGNED%"))
                .fetchOne(0, int.class);
        assertThat(reassignmentEvents).isGreaterThan(0);
    }

    @Test
    @DisplayName("Owner transfer does not reassign terminal deal owner")
    void ownerTransfer_terminalDeal_isNotReassigned() {
        TestDataFactory.upsertUser(dsl, NEW_OWNER_ID);
        UUID dealId = createDealViaApi(advertiserToken());

        dsl.update(DEALS)
                .set(DEALS.STATUS, "COMPLETED_RELEASED")
                .where(DEALS.ID.eq(dealId))
                .execute();
        dsl.update(CHANNELS)
                .set(CHANNELS.OWNER_ID, NEW_OWNER_ID)
                .where(CHANNELS.ID.eq(CHANNEL_ID))
                .execute();
        dsl.deleteFrom(CHANNEL_MEMBERSHIPS)
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(CHANNEL_ID))
                .execute();
        dsl.insertInto(CHANNEL_MEMBERSHIPS)
                .set(CHANNEL_MEMBERSHIPS.CHANNEL_ID, CHANNEL_ID)
                .set(CHANNEL_MEMBERSHIPS.USER_ID, NEW_OWNER_ID)
                .set(CHANNEL_MEMBERSHIPS.ROLE, "OWNER")
                .execute();

        when(channelAutoSyncPort.syncFromTelegram(CHANNEL_ID))
                .thenReturn(new ChannelSyncResult(
                        true, OWNER_ID, NEW_OWNER_ID));

        webClient.post()
                .uri("/api/v1/deals/{id}/transition", dealId)
                .headers(h -> h.setBearerAuth(newOwnerToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transitionRequest("ACCEPTED"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("INVALID_STATE_TRANSITION");

        Long dealOwnerId = dsl.select(DEALS.OWNER_ID)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchOne(DEALS.OWNER_ID);
        assertThat(dealOwnerId).isEqualTo(OWNER_ID);

        int reassignmentEvents = dsl.selectCount()
                .from(DEAL_EVENTS)
                .where(DEAL_EVENTS.DEAL_ID.eq(dealId))
                .and(DEAL_EVENTS.EVENT_TYPE.eq("AUDIT_EVENT"))
                .and(DSL.condition(
                        "payload::text like {0}",
                        "%OWNER_REASSIGNED%"))
                .fetchOne(0, int.class);
        assertThat(reassignmentEvents).isZero();
    }

    @Test
    @DisplayName("Manager with moderate right can execute owner-side transition")
    void managerWithModerateRight_canTransitionOwnerSide() {
        dsl.insertInto(CHANNEL_MEMBERSHIPS)
                .set(CHANNEL_MEMBERSHIPS.CHANNEL_ID, CHANNEL_ID)
                .set(CHANNEL_MEMBERSHIPS.USER_ID, MANAGER_ID)
                .set(CHANNEL_MEMBERSHIPS.ROLE, "MANAGER")
                .set(CHANNEL_MEMBERSHIPS.RIGHTS, JSONB.valueOf(
                        "{\"moderate\": true}"))
                .execute();

        UUID dealId = createDealViaApi(advertiserToken());
        transitionByToken(advertiserToken(), dealId, "OFFER_PENDING");

        webClient.post()
                .uri("/api/v1/deals/{id}/transition", dealId)
                .headers(h -> h.setBearerAuth(managerToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transitionRequest("ACCEPTED"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.newStatus").isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("Old owner after transfer cannot execute owner-side transition")
    void oldOwnerAfterTransfer_cannotTransitionOwnerSide() {
        TestDataFactory.upsertUser(dsl, NEW_OWNER_ID);
        UUID dealId = createDealViaApi(advertiserToken());
        transitionByToken(advertiserToken(), dealId, "OFFER_PENDING");

        dsl.update(CHANNELS)
                .set(CHANNELS.OWNER_ID, NEW_OWNER_ID)
                .where(CHANNELS.ID.eq(CHANNEL_ID))
                .execute();
        dsl.deleteFrom(CHANNEL_MEMBERSHIPS)
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(CHANNEL_ID))
                .execute();
        dsl.insertInto(CHANNEL_MEMBERSHIPS)
                .set(CHANNEL_MEMBERSHIPS.CHANNEL_ID, CHANNEL_ID)
                .set(CHANNEL_MEMBERSHIPS.USER_ID, NEW_OWNER_ID)
                .set(CHANNEL_MEMBERSHIPS.ROLE, "OWNER")
                .execute();

        when(channelAutoSyncPort.syncFromTelegram(CHANNEL_ID))
                .thenReturn(new ChannelSyncResult(
                        true, OWNER_ID, NEW_OWNER_ID));

        webClient.post()
                .uri("/api/v1/deals/{id}/transition", dealId)
                .headers(h -> h.setBearerAuth(ownerToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transitionRequest("ACCEPTED"))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error_code")
                .isEqualTo("DEAL_NOT_PARTICIPANT");
    }

    // --- helpers ---

    private String advertiserToken() {
        return TestDataFactory.jwt(jwtTokenProvider, ADVERTISER_ID);
    }

    private String ownerToken() {
        return TestDataFactory.jwt(jwtTokenProvider, OWNER_ID);
    }

    private String newOwnerToken() {
        return TestDataFactory.jwt(jwtTokenProvider, NEW_OWNER_ID);
    }

    private String managerToken() {
        return TestDataFactory.jwt(jwtTokenProvider, MANAGER_ID);
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

    private void transitionByToken(String token, UUID dealId, String targetStatus) {
        webClient.post()
                .uri("/api/v1/deals/{id}/transition", dealId)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transitionRequest(targetStatus))
                .exchange()
                .expectStatus().isOk();
    }

    private void transitionAsSystem(UUID dealId, DealStatus targetStatus) {
        var result = dealService.transition(new DealTransitionCommand(
                DealId.of(dealId),
                targetStatus,
                null,
                ActorType.SYSTEM,
                null,
                null,
                null));
        assertThat(result)
                .isInstanceOf(DealTransitionResult.Success.class);
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
                                null, 0, null,
                                new ChannelDetailResponse.ChannelRules(null),
                                List.of(),
                                OffsetDateTime.now(ZoneOffset.UTC),
                                OffsetDateTime.now(ZoneOffset.UTC)));
            });
            return repo;
        }

        @Bean
        ChannelAutoSyncPort channelAutoSyncPort() {
            return mock(ChannelAutoSyncPort.class);
        }

        @Bean
        CreativeRepository creativeRepository(DSLContext dsl, JsonFacade jsonFacade) {
            return new JooqCreativeTemplateRepository(dsl, jsonFacade);
        }

        @Bean
        ChannelAuthorizationPort channelAuthorizationPort(DSLContext dsl) {
            return new ChannelAuthorizationAdapter(dsl);
        }

        @Bean
        OutboxRepository outboxRepository(DSLContext dsl) {
            return new JooqOutboxRepository(dsl);
        }

        @Bean
        EscrowPort escrowPort() {
            return mock(EscrowPort.class);
        }

        @Bean
        DepositPort depositPort() {
            return mock(DepositPort.class);
        }

        @Bean
        EventEnvelopeDeserializer eventEnvelopeDeserializer() {
            return mock(EventEnvelopeDeserializer.class);
        }

        @Bean
        DistributedLockPort distributedLockPort() {
            return new DistributedLockPort() {
                @Override
                public Optional<String> tryLock(String key,
                                                java.time.Duration ttl) {
                    return Optional.of("test-lock-token");
                }

                @Override
                public void unlock(String key, String token) {
                    // no-op for tests
                }
            };
        }
    }
}

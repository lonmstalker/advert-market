package com.advertmarket.integration.deal;

import static com.advertmarket.db.generated.tables.DealEvents.DEAL_EVENTS;
import static com.advertmarket.db.generated.tables.Deals.DEALS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.app.outbox.JooqOutboxRepository;
import com.advertmarket.deal.config.DealTimeoutProperties;
import com.advertmarket.deal.mapper.DealEventRecordMapper;
import com.advertmarket.deal.mapper.DealRecordMapper;
import com.advertmarket.deal.repository.JooqDealEventRepository;
import com.advertmarket.deal.repository.JooqDealRepository;
import com.advertmarket.deal.service.DealTimeoutScheduler;
import com.advertmarket.deal.service.DealTransitionService;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.RedisSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.RedisDistributedLock;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.DealStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("DealTimeoutScheduler — integration")
class DealTimeoutSchedulerIt {

    private static final long ADVERTISER_ID = 101L;
    private static final long OWNER_ID = 202L;
    private static final long CHANNEL_ID = -1001234567890L;
    private static final long AMOUNT_NANO = 1_000_000_000L;
    private static final int COMMISSION_RATE_BP = 1000;
    private static final long COMMISSION_NANO = 100_000_000L;

    private static DSLContext dsl;

    private JooqDealRepository dealRepository;
    private DealTimeoutScheduler scheduler;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
        dsl = DatabaseSupport.dsl();
    }

    @BeforeEach
    void setUp() {
        DatabaseSupport.cleanAllTables(dsl);
        RedisSupport.flushAll();

        TestDataFactory.upsertUser(dsl, ADVERTISER_ID);
        TestDataFactory.upsertUser(dsl, OWNER_ID);
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);

        dealRepository = new JooqDealRepository(
                dsl,
                Mappers.getMapper(DealRecordMapper.class));
        var dealEventRepository = new JooqDealEventRepository(
                dsl,
                Mappers.getMapper(DealEventRecordMapper.class));
        var outboxRepository = new JooqOutboxRepository(dsl);
        var jsonFacade = new JsonFacade(
                new ObjectMapper().findAndRegisterModules());
        var transitionService = new DealTransitionService(
                dealRepository, dealEventRepository, outboxRepository, jsonFacade);
        var lockPort = new RedisDistributedLock(
                RedisSupport.redisTemplate(),
                new MetricsFacade(new SimpleMeterRegistry()));
        var metrics = new MetricsFacade(new SimpleMeterRegistry());
        var props = new DealTimeoutProperties(
                Duration.ofHours(48),
                Duration.ofHours(72),
                Duration.ofHours(24),
                Duration.ofHours(72),
                Duration.ofHours(72),
                Duration.ofHours(168),
                Duration.ofMinutes(5),
                50,
                Duration.ofMinutes(2));

        scheduler = new DealTimeoutScheduler(
                dealRepository,
                transitionService,
                lockPort,
                outboxRepository,
                jsonFacade,
                metrics,
                props);
    }

    @Test
    @DisplayName("Should not process deals inside grace period")
    void shouldNotProcessInsideGracePeriod() {
        var dealId = insertDeal(
                DealStatus.OFFER_PENDING,
                Instant.now().minus(Duration.ofMinutes(2)));

        scheduler.processExpiredDeals();

        assertThat(currentStatus(dealId)).isEqualTo(DealStatus.OFFER_PENDING);
        assertThat(currentDeadline(dealId)).isNotNull();
        assertThat(dsl.fetchCount(DEAL_EVENTS, DEAL_EVENTS.DEAL_ID.eq(dealId))).isZero();
    }

    @Test
    @DisplayName("Should clear deadline after transition to EXPIRED")
    void shouldClearDeadlineAfterExpiration() {
        var dealId = insertDeal(
                DealStatus.NEGOTIATING,
                Instant.now().minus(Duration.ofMinutes(10)));

        scheduler.processExpiredDeals();

        assertThat(currentStatus(dealId)).isEqualTo(DealStatus.EXPIRED);
        assertThat(currentDeadline(dealId)).isNull();
    }

    private UUID insertDeal(DealStatus status, Instant deadlineAt) {
        var dealId = UUID.randomUUID();
        dsl.insertInto(DEALS)
                .set(DEALS.ID, dealId)
                .set(DEALS.CHANNEL_ID, CHANNEL_ID)
                .set(DEALS.ADVERTISER_ID, ADVERTISER_ID)
                .set(DEALS.OWNER_ID, OWNER_ID)
                .set(DEALS.STATUS, status.name())
                .set(DEALS.AMOUNT_NANO, AMOUNT_NANO)
                .set(DEALS.COMMISSION_RATE_BP, COMMISSION_RATE_BP)
                .set(DEALS.COMMISSION_NANO, COMMISSION_NANO)
                .set(DEALS.DEADLINE_AT, OffsetDateTime.ofInstant(deadlineAt, ZoneOffset.UTC))
                .execute();
        return dealId;
    }

    private DealStatus currentStatus(UUID dealId) {
        var status = dsl.select(DEALS.STATUS)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchOne(DEALS.STATUS);
        return DealStatus.valueOf(status);
    }

    private OffsetDateTime currentDeadline(UUID dealId) {
        return dsl.select(DEALS.DEADLINE_AT)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId))
                .fetchOne(DEALS.DEADLINE_AT);
    }
}

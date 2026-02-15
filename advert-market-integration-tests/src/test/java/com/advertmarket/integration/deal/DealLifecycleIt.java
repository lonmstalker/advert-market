package com.advertmarket.integration.deal;

import static com.advertmarket.db.generated.tables.DealEvents.DEAL_EVENTS;
import static com.advertmarket.db.generated.tables.Deals.DEALS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.deal.api.dto.DealEventRecord;
import com.advertmarket.deal.api.dto.DealListCriteria;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.mapper.DealEventRecordMapper;
import com.advertmarket.deal.mapper.DealRecordMapper;
import com.advertmarket.deal.repository.JooqDealEventRepository;
import com.advertmarket.deal.repository.JooqDealRepository;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import java.time.Instant;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("Deal Lifecycle — PostgreSQL integration")
class DealLifecycleIt {

    private static DSLContext dsl;
    private JooqDealRepository dealRepo;
    private JooqDealEventRepository eventRepo;

    private static final long ADVERTISER_ID = 100L;
    private static final long OWNER_ID = 200L;
    private static final long CHANNEL_ID = -1001234567890L;
    private static final long OTHER_USER_ID = 999L;

    private static final long ONE_TON_NANO = 1_000_000_000L;
    private static final int COMMISSION_RATE_BP = 1000;
    private static final long COMMISSION_NANO = 100_000_000L;

    private static final int WRONG_VERSION = 99;
    private static final int PAGE_LIMIT = 20;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
        dsl = DatabaseSupport.dsl();
    }

    @BeforeEach
    void setUp() {
        DatabaseSupport.cleanAllTables(dsl);
        TestDataFactory.upsertUser(dsl, ADVERTISER_ID);
        TestDataFactory.upsertUser(dsl, OWNER_ID);
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, OWNER_ID);

        dealRepo = new JooqDealRepository(
                dsl, Mappers.getMapper(DealRecordMapper.class));
        eventRepo = new JooqDealEventRepository(
                dsl, Mappers.getMapper(DealEventRecordMapper.class));
    }

    private DealRecord createDraft(DealId dealId) {
        var now = Instant.now();
        return new DealRecord(
                dealId.value(), CHANNEL_ID, ADVERTISER_ID, OWNER_ID, null,
                DealStatus.DRAFT, ONE_TON_NANO, COMMISSION_RATE_BP, COMMISSION_NANO,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                0, now, now);
    }

    @Nested
    @DisplayName("DealRepository")
    class DealRepositoryTests {

        @Test
        @DisplayName("insert and findById should round-trip deal record")
        void insertAndFindById_shouldRoundTrip() {
            var dealId = DealId.generate();
            dealRepo.insert(createDraft(dealId));

            var found = dealRepo.findById(dealId);

            assertThat(found).isPresent();
            var deal = found.get();
            assertThat(deal.id()).isEqualTo(dealId.value());
            assertThat(deal.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(deal.advertiserId()).isEqualTo(ADVERTISER_ID);
            assertThat(deal.ownerId()).isEqualTo(OWNER_ID);
            assertThat(deal.status()).isEqualTo(DealStatus.DRAFT);
            assertThat(deal.amountNano()).isEqualTo(ONE_TON_NANO);
            assertThat(deal.commissionRateBp()).isEqualTo(COMMISSION_RATE_BP);
            assertThat(deal.commissionNano()).isEqualTo(COMMISSION_NANO);
            assertThat(deal.version()).isEqualTo(0);
        }

        @Test
        @DisplayName("updateStatus with correct version should update and return 1")
        void updateStatus_correctVersion_shouldSucceed() {
            var dealId = DealId.generate();
            dealRepo.insert(createDraft(dealId));

            int updated = dealRepo.updateStatus(
                    dealId, DealStatus.DRAFT, DealStatus.OFFER_PENDING, 0);

            assertThat(updated).isEqualTo(1);

            var deal = dealRepo.findById(dealId).orElseThrow();
            assertThat(deal.status()).isEqualTo(DealStatus.OFFER_PENDING);
            assertThat(deal.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("updateStatus with wrong version should return 0 (CAS failure)")
        void updateStatus_wrongVersion_shouldReturn0() {
            var dealId = DealId.generate();
            dealRepo.insert(createDraft(dealId));

            int updated = dealRepo.updateStatus(
                    dealId, DealStatus.DRAFT, DealStatus.OFFER_PENDING, WRONG_VERSION);

            assertThat(updated).isEqualTo(0);

            var deal = dealRepo.findById(dealId).orElseThrow();
            assertThat(deal.status()).isEqualTo(DealStatus.DRAFT);
            assertThat(deal.version()).isEqualTo(0);
        }

        @Test
        @DisplayName("updateStatus with wrong expected status should return 0")
        void updateStatus_wrongExpectedStatus_shouldReturn0() {
            var dealId = DealId.generate();
            dealRepo.insert(createDraft(dealId));

            int updated = dealRepo.updateStatus(
                    dealId, DealStatus.FUNDED, DealStatus.OFFER_PENDING, 0);

            assertThat(updated).isEqualTo(0);
        }

        @Test
        @DisplayName("listByUser should return deals for advertiser or owner")
        void listByUser_shouldReturnDealsForUser() {
            var deal1 = DealId.generate();
            var deal2 = DealId.generate();
            dealRepo.insert(createDraft(deal1));
            dealRepo.insert(createDraft(deal2));

            var criteria = new DealListCriteria(null, null, PAGE_LIMIT);
            var advertiserDeals = dealRepo.listByUser(ADVERTISER_ID, criteria);
            var ownerDeals = dealRepo.listByUser(OWNER_ID, criteria);
            var otherDeals = dealRepo.listByUser(OTHER_USER_ID, criteria);

            assertThat(advertiserDeals).hasSize(2);
            assertThat(ownerDeals).hasSize(2);
            assertThat(otherDeals).isEmpty();
        }

        @Test
        @DisplayName("listByUser with status filter should filter correctly")
        void listByUser_withStatusFilter_shouldFilter() {
            var deal1 = DealId.generate();
            var deal2 = DealId.generate();
            dealRepo.insert(createDraft(deal1));
            dealRepo.insert(createDraft(deal2));
            dealRepo.updateStatus(deal1, DealStatus.DRAFT,
                    DealStatus.OFFER_PENDING, 0);

            var draftOnly = new DealListCriteria(
                    DealStatus.DRAFT, null, PAGE_LIMIT);
            var result = dealRepo.listByUser(ADVERTISER_ID, draftOnly);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo(deal2.value());
        }
    }

    @Nested
    @DisplayName("DealEventRepository")
    class DealEventRepositoryTests {

        @Test
        @DisplayName("append and findByDealId should persist events")
        void appendAndFind_shouldPersistEvents() {
            var dealId = DealId.generate();
            dealRepo.insert(createDraft(dealId));

            eventRepo.append(new DealEventRecord(
                    null, dealId.value(), "DEAL_STATE_CHANGED",
                    "DRAFT", "OFFER_PENDING", ADVERTISER_ID,
                    "ADVERTISER", "{}", Instant.now()));

            eventRepo.append(new DealEventRecord(
                    null, dealId.value(), "DEAL_STATE_CHANGED",
                    "OFFER_PENDING", "ACCEPTED", OWNER_ID,
                    "CHANNEL_OWNER", "{}", Instant.now()));

            var events = eventRepo.findByDealId(dealId);

            assertThat(events).hasSize(2);
            assertThat(events.getFirst().toStatus()).isEqualTo("ACCEPTED");
            assertThat(events.getLast().toStatus()).isEqualTo("OFFER_PENDING");
        }
    }

    @Nested
    @DisplayName("Full lifecycle")
    class FullLifecycle {

        @Test
        @DisplayName("deal should transition through DRAFT → OFFER_PENDING → ACCEPTED")
        void fullLifecycle_draftToAccepted() {
            var dealId = DealId.generate();
            dealRepo.insert(createDraft(dealId));

            // DRAFT → OFFER_PENDING
            assertThat(dealRepo.updateStatus(
                    dealId, DealStatus.DRAFT, DealStatus.OFFER_PENDING, 0))
                    .isEqualTo(1);
            eventRepo.append(new DealEventRecord(
                    null, dealId.value(), "DEAL_STATE_CHANGED",
                    "DRAFT", "OFFER_PENDING", ADVERTISER_ID,
                    "ADVERTISER", "{}", Instant.now()));

            // OFFER_PENDING → ACCEPTED
            assertThat(dealRepo.updateStatus(
                    dealId, DealStatus.OFFER_PENDING, DealStatus.ACCEPTED, 1))
                    .isEqualTo(1);
            eventRepo.append(new DealEventRecord(
                    null, dealId.value(), "DEAL_STATE_CHANGED",
                    "OFFER_PENDING", "ACCEPTED", OWNER_ID,
                    "CHANNEL_OWNER", "{}", Instant.now()));

            var deal = dealRepo.findById(dealId).orElseThrow();
            assertThat(deal.status()).isEqualTo(DealStatus.ACCEPTED);
            assertThat(deal.version()).isEqualTo(2);

            var events = eventRepo.findByDealId(dealId);
            assertThat(events).hasSize(2);
        }
    }
}

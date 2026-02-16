package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.PricingRulePostTypes.PRICING_RULE_POST_TYPES;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.dto.NewChannel;
import com.advertmarket.marketplace.channel.mapper.CategoryDtoMapper;
import com.advertmarket.marketplace.channel.mapper.ChannelRecordMapper;
import com.advertmarket.marketplace.channel.repository.JooqCategoryRepository;
import com.advertmarket.marketplace.channel.repository.JooqChannelRepository;
import com.advertmarket.marketplace.pricing.mapper.PricingRuleRecordMapper;
import com.advertmarket.marketplace.pricing.repository.JooqPricingRuleRepository;
import com.advertmarket.shared.json.JsonFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * Integration test for JooqChannelRepository with real PostgreSQL.
 */
@DisplayName("JooqChannelRepository — PostgreSQL integration")
class JooqChannelRepositoryIntegrationTest {

    private static final long TEST_USER_ID = 1L;
    private static final long CHANNEL_ID = -100L;
    private static final String CHANNEL_TITLE = "Chan A";
    private static final String CHANNEL_USERNAME = "chan_a";

    private static DSLContext dsl;
    private JooqChannelRepository repository;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
        dsl = DatabaseSupport.dsl();
    }

    @BeforeEach
    void setUp() {
        var jsonFacade = new JsonFacade(new ObjectMapper());
        var categoryRepo = new JooqCategoryRepository(
                dsl,
                jsonFacade,
                Mappers.getMapper(CategoryDtoMapper.class));
        var pricingRuleMapper = Mappers.getMapper(PricingRuleRecordMapper.class);
        var pricingRuleRepo = new JooqPricingRuleRepository(dsl, pricingRuleMapper);
        repository = new JooqChannelRepository(
                dsl,
                Mappers.getMapper(ChannelRecordMapper.class),
                categoryRepo,
                pricingRuleRepo);
        DatabaseSupport.cleanAllTables(dsl);
        TestDataFactory.upsertUser(dsl, TEST_USER_ID);
    }

    @Test
    @DisplayName("Should return false for non-existent channel")
    void shouldReturnFalseForNonExistentChannel() {
        assertThat(repository.existsByTelegramId(CHANNEL_ID))
                .isFalse();
    }

    @Test
    @DisplayName("Should insert channel and owner membership")
    void shouldInsertChannelAndMembership() {
        ChannelResponse result = repository.insert(testChannel());

        assertThat(result.id()).isEqualTo(CHANNEL_ID);
        assertThat(result.title()).isEqualTo(CHANNEL_TITLE);
        assertThat(result.username()).isEqualTo(CHANNEL_USERNAME);
        assertThat(result.ownerId()).isEqualTo(TEST_USER_ID);
        assertThat(result.isActive()).isTrue();
        assertThat(result.createdAt()).isNotNull();

        int membershipCount = dsl.fetchCount(
                CHANNEL_MEMBERSHIPS,
                CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(CHANNEL_ID)
                        .and(CHANNEL_MEMBERSHIPS.USER_ID
                                .eq(TEST_USER_ID))
                        .and(CHANNEL_MEMBERSHIPS.ROLE
                                .eq("OWNER")));
        assertThat(membershipCount).isOne();
    }

    @Test
    @DisplayName("Should return true after insert")
    void shouldReturnTrueAfterInsert() {
        repository.insert(testChannel());

        assertThat(repository.existsByTelegramId(CHANNEL_ID))
                .isTrue();
    }

    @Test
    @DisplayName("Should find channel by Telegram ID")
    void shouldFindByTelegramId() {
        repository.insert(testChannel());

        Optional<ChannelResponse> found =
                repository.findByTelegramId(CHANNEL_ID);

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(CHANNEL_ID);
        assertThat(found.get().title()).isEqualTo(CHANNEL_TITLE);
        assertThat(found.get().subscriberCount()).isEqualTo(5000);
        assertThat(found.get().categories()).contains("tech");
    }

    @Test
    @DisplayName("Should return empty for non-existent find")
    void shouldReturnEmptyForNonExistentFind() {
        Optional<ChannelResponse> found =
                repository.findByTelegramId(CHANNEL_ID);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find detail by ID with pricing rules")
    void shouldFindDetailByIdWithPricingRules() {
        repository.insert(testChannel());
        TestDataFactory.insertPricingRule(dsl, CHANNEL_ID, "Repost", "REPOST", 1_000_000L, 1);
        TestDataFactory.insertPricingRule(dsl, CHANNEL_ID, "Native", "NATIVE", 2_000_000L, 2);

        Optional<ChannelDetailResponse> detail =
                repository.findDetailById(CHANNEL_ID);

        assertThat(detail).isPresent();
        var ch = detail.get();
        assertThat(ch.id()).isEqualTo(CHANNEL_ID);
        assertThat(ch.title()).isEqualTo(CHANNEL_TITLE);
        assertThat(ch.username()).isEqualTo(CHANNEL_USERNAME);
        assertThat(ch.subscriberCount()).isEqualTo(5000);
        assertThat(ch.categories()).contains("tech");
        assertThat(ch.ownerId()).isEqualTo(TEST_USER_ID);
        assertThat(ch.isActive()).isTrue();
        assertThat(ch.pricingRules()).hasSize(2);
        assertThat(ch.pricingRules().get(0).name()).isEqualTo("Repost");
        assertThat(ch.pricingRules().get(1).name()).isEqualTo("Native");
    }

    @Test
    @DisplayName("Should return empty detail for non-existent channel")
    void shouldReturnEmptyDetailForNonExistent() {
        assertThat(repository.findDetailById(999L)).isEmpty();
    }

    @Test
    @DisplayName("Should update channel fields")
    void shouldUpdateChannelFields() {
        repository.insert(testChannel());

        var request = new ChannelUpdateRequest(
                "Updated desc", List.of("crypto"), 5_000_000L, "ru", null, null);
        Optional<ChannelResponse> updated =
                repository.update(CHANNEL_ID, request);

        assertThat(updated).isPresent();
        var ch = updated.get();
        assertThat(ch.description()).isEqualTo("Updated desc");
        assertThat(ch.categories()).contains("crypto");
        assertThat(ch.pricePerPostNano()).isEqualTo(5_000_000L);
    }

    @Test
    @DisplayName("Should return empty update for non-existent channel")
    void shouldReturnEmptyUpdateForNonExistent() {
        var request = new ChannelUpdateRequest(
                "desc", null, null, null, null, null);

        assertThat(repository.update(999L, request)).isEmpty();
    }

    @Test
    @DisplayName("Should deactivate channel")
    void shouldDeactivateChannel() {
        repository.insert(testChannel());

        boolean result = repository.deactivate(CHANNEL_ID);

        assertThat(result).isTrue();
        var record = dsl.selectFrom(CHANNELS)
                .where(CHANNELS.ID.eq(CHANNEL_ID))
                .fetchOne();
        assertThat(record).isNotNull();
        assertThat(record.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should return false for deactivating already inactive channel")
    void shouldReturnFalseForDeactivatingInactive() {
        repository.insert(testChannel());
        repository.deactivate(CHANNEL_ID);

        assertThat(repository.deactivate(CHANNEL_ID)).isFalse();
    }

    @Test
    @DisplayName("Should return false for deactivating non-existent channel")
    void shouldReturnFalseForDeactivatingNonExistent() {
        assertThat(repository.deactivate(999L)).isFalse();
    }

    private static NewChannel testChannel() {
        return new NewChannel(
                CHANNEL_ID, CHANNEL_TITLE, CHANNEL_USERNAME,
                "Test description", 5000, List.of("tech"),
                null, TEST_USER_ID);
    }
}

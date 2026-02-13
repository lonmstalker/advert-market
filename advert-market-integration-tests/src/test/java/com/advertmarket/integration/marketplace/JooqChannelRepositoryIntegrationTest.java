package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.PricingRulePostTypes.PRICING_RULE_POST_TYPES;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.dto.NewChannel;
import com.advertmarket.marketplace.channel.mapper.ChannelRecordMapper;
import com.advertmarket.marketplace.channel.repository.JooqCategoryRepository;
import com.advertmarket.marketplace.channel.repository.JooqChannelRepository;
import com.advertmarket.marketplace.pricing.mapper.PricingRuleRecordMapper;
import com.advertmarket.marketplace.pricing.repository.JooqPricingRuleRepository;
import com.advertmarket.shared.json.JsonFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
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
import org.mapstruct.factory.Mappers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for JooqChannelRepository with real PostgreSQL.
 */
@Testcontainers
@DisplayName("JooqChannelRepository — PostgreSQL integration")
class JooqChannelRepositoryIntegrationTest {

    private static final long TEST_USER_ID = 1L;
    private static final long CHANNEL_ID = -100L;
    private static final String CHANNEL_TITLE = "Chan A";
    private static final String CHANNEL_USERNAME = "chan_a";

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    private static DSLContext dsl;
    private JooqChannelRepository repository;

    @BeforeAll
    static void initDatabase() throws Exception {
        dsl = DSL.using(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());

        var conn = dsl.configuration().connectionProvider().acquire();
        var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(
                        new JdbcConnection(conn));
        var liquibase = new Liquibase(
                "db/changelog/db.changelog-master.yaml",
                new ClassLoaderResourceAccessor(),
                database);
        liquibase.update("");
    }

    @BeforeEach
    void setUp() {
        var jsonFacade = new JsonFacade(new ObjectMapper());
        var categoryRepo = new JooqCategoryRepository(dsl, jsonFacade);
        var pricingRuleMapper = Mappers.getMapper(PricingRuleRecordMapper.class);
        var pricingRuleRepo = new JooqPricingRuleRepository(dsl, pricingRuleMapper);
        repository = new JooqChannelRepository(
                dsl,
                Mappers.getMapper(ChannelRecordMapper.class),
                pricingRuleMapper,
                categoryRepo,
                pricingRuleRepo);
        dsl.deleteFrom(PRICING_RULE_POST_TYPES).execute();
        dsl.deleteFrom(CHANNEL_PRICING_RULES).execute();
        dsl.deleteFrom(CHANNEL_CATEGORIES).execute();
        dsl.deleteFrom(CHANNEL_MEMBERSHIPS).execute();
        dsl.deleteFrom(CHANNELS).execute();
        dsl.deleteFrom(USERS).execute();
        insertTestUser(TEST_USER_ID);
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
        insertPricingRule(CHANNEL_ID, "Repost", "REPOST", 1_000_000L, 1);
        insertPricingRule(CHANNEL_ID, "Native", "NATIVE", 2_000_000L, 2);

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
                "Updated desc", List.of("crypto"), 5_000_000L, "ru", null);
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
                "desc", null, null, null, null);

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

    private static void insertPricingRule(long channelId, String name,
                                          String postType, long priceNano,
                                          int sortOrder) {
        long ruleId = dsl.insertInto(CHANNEL_PRICING_RULES)
                .set(CHANNEL_PRICING_RULES.CHANNEL_ID, channelId)
                .set(CHANNEL_PRICING_RULES.NAME, name)
                .set(CHANNEL_PRICING_RULES.PRICE_NANO, priceNano)
                .set(CHANNEL_PRICING_RULES.SORT_ORDER, sortOrder)
                .returning(CHANNEL_PRICING_RULES.ID)
                .fetchSingle()
                .getId();
        dsl.insertInto(PRICING_RULE_POST_TYPES)
                .set(PRICING_RULE_POST_TYPES.PRICING_RULE_ID, ruleId)
                .set(PRICING_RULE_POST_TYPES.POST_TYPE, postType)
                .execute();
    }

    private static NewChannel testChannel() {
        return new NewChannel(
                CHANNEL_ID, CHANNEL_TITLE, CHANNEL_USERNAME,
                "Test description", 5000, List.of("tech"),
                null, TEST_USER_ID);
    }

    private static void insertTestUser(long userId) {
        dsl.insertInto(USERS)
                .set(USERS.ID, userId)
                .set(USERS.FIRST_NAME, "Test")
                .set(USERS.LANGUAGE_CODE, "en")
                .onConflictDoNothing()
                .execute();
    }
}

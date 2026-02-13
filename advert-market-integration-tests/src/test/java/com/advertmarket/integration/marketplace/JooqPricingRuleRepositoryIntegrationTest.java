package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.PricingRulePostTypes.PRICING_RULE_POST_TYPES;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.marketplace.api.dto.PricingRuleCreateRequest;
import com.advertmarket.marketplace.api.dto.PricingRuleDto;
import com.advertmarket.marketplace.api.dto.PricingRuleUpdateRequest;
import com.advertmarket.marketplace.api.model.PostType;
import com.advertmarket.marketplace.pricing.mapper.PricingRuleRecordMapper;
import com.advertmarket.marketplace.pricing.repository.JooqPricingRuleRepository;
import java.util.List;
import java.util.Set;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * Integration test for JooqPricingRuleRepository with real PostgreSQL.
 */
@DisplayName("JooqPricingRuleRepository — PostgreSQL integration")
class JooqPricingRuleRepositoryIntegrationTest {

    private static final long TEST_USER_ID = 1L;
    private static final long CHANNEL_ID = -100L;

    private static DSLContext dsl;
    private JooqPricingRuleRepository repository;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
        dsl = DatabaseSupport.dsl();
    }

    @BeforeEach
    void setUp() {
        repository = new JooqPricingRuleRepository(
                dsl,
                Mappers.getMapper(PricingRuleRecordMapper.class));
        DatabaseSupport.cleanAllTables(dsl);
        TestDataFactory.upsertUser(dsl, TEST_USER_ID);
        TestDataFactory.insertChannelWithOwner(dsl, CHANNEL_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("Should return empty list for channel with no rules")
    void shouldReturnEmptyListForChannelWithNoRules() {
        List<PricingRuleDto> rules =
                repository.findByChannelId(CHANNEL_ID);

        assertThat(rules).isEmpty();
    }

    @Test
    @DisplayName("Should return active rules sorted by sort order")
    void shouldReturnActiveRulesSortedBySortOrder() {
        repository.insert(CHANNEL_ID,
                createRule("B", Set.of(PostType.REPOST), 2_000_000L, 2));
        repository.insert(CHANNEL_ID,
                createRule("A", Set.of(PostType.NATIVE), 1_000_000L, 1));
        repository.insert(CHANNEL_ID,
                createRule("C", Set.of(PostType.STORY), 3_000_000L, 3));

        List<PricingRuleDto> rules =
                repository.findByChannelId(CHANNEL_ID);

        assertThat(rules).hasSize(3);
        assertThat(rules.get(0).name()).isEqualTo("A");
        assertThat(rules.get(1).name()).isEqualTo("B");
        assertThat(rules.get(2).name()).isEqualTo("C");
    }

    @Test
    @DisplayName("Should exclude inactive rules from findByChannelId")
    void shouldExcludeInactiveRules() {
        PricingRuleDto active = repository.insert(
                CHANNEL_ID,
                createRule("Active", Set.of(PostType.REPOST), 1_000_000L, 1));
        PricingRuleDto toDeactivate = repository.insert(
                CHANNEL_ID,
                createRule("Inactive", Set.of(PostType.NATIVE), 2_000_000L, 2));
        repository.deactivate(toDeactivate.id());

        List<PricingRuleDto> rules =
                repository.findByChannelId(CHANNEL_ID);

        assertThat(rules).hasSize(1);
        assertThat(rules.getFirst().name()).isEqualTo("Active");
    }

    @Test
    @DisplayName("Should find rule by ID")
    void shouldFindRuleById() {
        PricingRuleDto created = repository.insert(
                CHANNEL_ID,
                createRule("Repost", Set.of(PostType.REPOST), 1_500_000L, 1));

        var found = repository.findById(created.id());

        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Repost");
        assertThat(found.get().postTypes()).containsExactly(PostType.REPOST);
        assertThat(found.get().priceNano()).isEqualTo(1_500_000L);
        assertThat(found.get().channelId()).isEqualTo(CHANNEL_ID);
        assertThat(found.get().isActive()).isTrue();
        assertThat(found.get().sortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should insert rule with all fields")
    void shouldInsertRule() {
        PricingRuleDto created = repository.insert(
                CHANNEL_ID,
                new PricingRuleCreateRequest(
                        "Native Ad", "Full native integration",
                        Set.of(PostType.NATIVE), 5_000_000L, 3));

        assertThat(created.id()).isPositive();
        assertThat(created.channelId()).isEqualTo(CHANNEL_ID);
        assertThat(created.name()).isEqualTo("Native Ad");
        assertThat(created.description()).isEqualTo("Full native integration");
        assertThat(created.postTypes()).containsExactly(PostType.NATIVE);
        assertThat(created.priceNano()).isEqualTo(5_000_000L);
        assertThat(created.sortOrder()).isEqualTo(3);
        assertThat(created.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should update rule partially")
    void shouldUpdateRulePartially() {
        PricingRuleDto created = repository.insert(
                CHANNEL_ID,
                createRule("Old Name", Set.of(PostType.REPOST), 1_000_000L, 1));

        var updateReq = new PricingRuleUpdateRequest(
                "New Name", null, null, null, null, null);
        var updated = repository.update(created.id(), updateReq);

        assertThat(updated).isPresent();
        assertThat(updated.get().name()).isEqualTo("New Name");
        assertThat(updated.get().postTypes()).containsExactly(PostType.REPOST);
        assertThat(updated.get().priceNano()).isEqualTo(1_000_000L);
        assertThat(updated.get().sortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should deactivate rule")
    void shouldDeactivateRule() {
        PricingRuleDto created = repository.insert(
                CHANNEL_ID,
                createRule("Rule", Set.of(PostType.REPOST), 1_000_000L, 1));

        boolean result = repository.deactivate(created.id());

        assertThat(result).isTrue();
        var record = dsl.selectFrom(CHANNEL_PRICING_RULES)
                .where(CHANNEL_PRICING_RULES.ID.eq(created.id()))
                .fetchOne();
        assertThat(record).isNotNull();
        assertThat(record.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should return false for deactivating already inactive rule")
    void shouldReturnFalseForDeactivatingInactiveRule() {
        PricingRuleDto created = repository.insert(
                CHANNEL_ID,
                createRule("Rule", Set.of(PostType.REPOST), 1_000_000L, 1));
        repository.deactivate(created.id());

        assertThat(repository.deactivate(created.id())).isFalse();
    }

    // --- helpers ---

    private static PricingRuleCreateRequest createRule(
            String name, Set<PostType> postTypes, long priceNano,
            int sortOrder) {
        return new PricingRuleCreateRequest(
                name, null, postTypes, priceNano, sortOrder);
    }
}

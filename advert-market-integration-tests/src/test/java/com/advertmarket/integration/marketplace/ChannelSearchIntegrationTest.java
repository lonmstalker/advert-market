package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.Categories.CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.marketplace.api.dto.ChannelListItem;
import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import com.advertmarket.marketplace.api.dto.ChannelSort;
import com.advertmarket.marketplace.channel.repository.JooqCategoryRepository;
import com.advertmarket.marketplace.channel.search.ParadeDbChannelSearch;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.pagination.CursorPage;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for ParadeDbChannelSearch filter-based search.
 *
 * <p>BM25 text search ({@code channels @@@}) requires ParadeDB image
 * and is not tested here. Only filter/sort/pagination logic is covered.
 */
@Testcontainers
@DisplayName("ParadeDbChannelSearch — filter-based search integration")
class ChannelSearchIntegrationTest {

    private static final long USER_ID = 1L;

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    private static DSLContext dsl;
    private ParadeDbChannelSearch search;

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
        search = new ParadeDbChannelSearch(dsl, categoryRepo);
        dsl.deleteFrom(CHANNEL_CATEGORIES).execute();
        dsl.deleteFrom(CHANNEL_PRICING_RULES).execute();
        dsl.deleteFrom(CHANNEL_MEMBERSHIPS).execute();
        dsl.deleteFrom(CHANNELS).execute();
        dsl.deleteFrom(USERS).execute();
        insertTestUser(USER_ID);
    }

    @Test
    @DisplayName("Should return active channels only")
    void shouldReturnActiveChannelsOnly() {
        insertChannel(-1L, "Active 1", "tech", 1000, null, true);
        insertChannel(-2L, "Active 2", "crypto", 2000, null, true);
        insertChannel(-3L, "Inactive", "tech", 500, null, false);

        CursorPage<ChannelListItem> page = search.search(criteria(
                null, null, null, null, null,
                ChannelSort.SUBSCRIBERS_DESC, null, 20));

        assertThat(page.items()).hasSize(2);
        assertThat(page.items()).allMatch(ChannelListItem::isActive);
    }

    @Test
    @DisplayName("Should filter by category")
    void shouldFilterByCategory() {
        insertChannel(-1L, "Tech Chan", "tech", 1000, null, true);
        insertChannel(-2L, "Crypto Chan", "crypto", 2000, null, true);

        CursorPage<ChannelListItem> page = search.search(criteria(
                "tech", null, null, null, null,
                ChannelSort.SUBSCRIBERS_DESC, null, 20));

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().categories()).contains("tech");
    }

    @Test
    @DisplayName("Should filter by subscriber range")
    void shouldFilterBySubscriberRange() {
        insertChannel(-1L, "Small", "tech", 100, null, true);
        insertChannel(-2L, "Medium", "tech", 500, null, true);
        insertChannel(-3L, "Large", "tech", 1000, null, true);

        CursorPage<ChannelListItem> page = search.search(criteria(
                null, 200, 800, null, null,
                ChannelSort.SUBSCRIBERS_DESC, null, 20));

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().subscriberCount()).isEqualTo(500);
    }

    @Test
    @DisplayName("Should filter by price range")
    void shouldFilterByPriceRange() {
        insertChannel(-1L, "Cheap", "tech", 1000, 100_000L, true);
        insertChannel(-2L, "Mid", "tech", 1000, 500_000L, true);
        insertChannel(-3L, "Expensive", "tech", 1000, 1_000_000L, true);

        CursorPage<ChannelListItem> page = search.search(criteria(
                null, null, null, 200_000L, 800_000L,
                ChannelSort.SUBSCRIBERS_DESC, null, 20));

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().pricePerPostNano())
                .isEqualTo(500_000L);
    }

    @Test
    @DisplayName("Should sort by subscribers desc")
    void shouldSortBySubscribersDesc() {
        insertChannel(-1L, "Small", "tech", 100, null, true);
        insertChannel(-2L, "Large", "tech", 1000, null, true);
        insertChannel(-3L, "Medium", "tech", 500, null, true);

        CursorPage<ChannelListItem> page = search.search(criteria(
                null, null, null, null, null,
                ChannelSort.SUBSCRIBERS_DESC, null, 20));

        assertThat(page.items()).hasSize(3);
        assertThat(page.items().get(0).subscriberCount()).isEqualTo(1000);
        assertThat(page.items().get(1).subscriberCount()).isEqualTo(500);
        assertThat(page.items().get(2).subscriberCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should sort by price asc")
    void shouldSortByPriceAsc() {
        insertChannel(-1L, "Expensive", "tech", 1000, 3_000_000L, true);
        insertChannel(-2L, "Cheap", "tech", 1000, 1_000_000L, true);
        insertChannel(-3L, "Mid", "tech", 1000, 2_000_000L, true);

        CursorPage<ChannelListItem> page = search.search(criteria(
                null, null, null, null, null,
                ChannelSort.PRICE_ASC, null, 20));

        assertThat(page.items()).hasSize(3);
        assertThat(page.items().get(0).pricePerPostNano()).isEqualTo(1_000_000L);
        assertThat(page.items().get(1).pricePerPostNano()).isEqualTo(2_000_000L);
        assertThat(page.items().get(2).pricePerPostNano()).isEqualTo(3_000_000L);
    }

    @Test
    @DisplayName("Should paginate with cursor")
    void shouldPaginateWithCursor() {
        for (int i = 1; i <= 5; i++) {
            insertChannel(-i, "Chan " + i, "tech", i * 1000, null, true);
        }

        CursorPage<ChannelListItem> page1 = search.search(criteria(
                null, null, null, null, null,
                ChannelSort.SUBSCRIBERS_DESC, null, 2));

        assertThat(page1.items()).hasSize(2);
        assertThat(page1.nextCursor()).isNotNull();

        CursorPage<ChannelListItem> page2 = search.search(criteria(
                null, null, null, null, null,
                ChannelSort.SUBSCRIBERS_DESC, page1.nextCursor(), 2));

        assertThat(page2.items()).hasSize(2);
        assertThat(page2.items().getFirst().subscriberCount())
                .isLessThan(page1.items().getLast().subscriberCount());
    }

    @Test
    @DisplayName("Should return empty page when no match")
    void shouldReturnEmptyPageWhenNoMatch() {
        insertChannel(-1L, "Tech", "tech", 1000, null, true);

        CursorPage<ChannelListItem> page = search.search(criteria(
                "nonexistent", null, null, null, null,
                ChannelSort.SUBSCRIBERS_DESC, null, 20));

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    // --- helpers ---

    private static ChannelSearchCriteria criteria(
            String category, Integer minSub, Integer maxSub,
            Long minPrice, Long maxPrice, ChannelSort sort,
            String cursor, int limit) {
        return new ChannelSearchCriteria(
                category, minSub, maxSub, minPrice, maxPrice,
                null, null, null, sort, cursor, limit);
    }

    private static void insertChannel(long id, String title, String categorySlug,
                                       int subscriberCount, Long priceNano,
                                       boolean isActive) {
        dsl.insertInto(CHANNELS)
                .set(CHANNELS.ID, id)
                .set(CHANNELS.TITLE, title)
                .set(CHANNELS.SUBSCRIBER_COUNT, subscriberCount)
                .set(CHANNELS.PRICE_PER_POST_NANO, priceNano)
                .set(CHANNELS.IS_ACTIVE, isActive)
                .set(CHANNELS.OWNER_ID, USER_ID)
                .execute();
        if (categorySlug != null) {
            Integer catId = dsl.select(CATEGORIES.ID)
                    .from(CATEGORIES)
                    .where(CATEGORIES.SLUG.eq(categorySlug))
                    .fetchOneInto(Integer.class);
            if (catId != null) {
                dsl.insertInto(CHANNEL_CATEGORIES)
                        .set(CHANNEL_CATEGORIES.CHANNEL_ID, id)
                        .set(CHANNEL_CATEGORIES.CATEGORY_ID, catId)
                        .execute();
            }
        }
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

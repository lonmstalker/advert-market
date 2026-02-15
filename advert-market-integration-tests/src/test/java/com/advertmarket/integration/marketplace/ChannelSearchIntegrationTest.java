package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.Categories.CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.SharedContainers;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.marketplace.api.dto.ChannelListItem;
import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import com.advertmarket.marketplace.api.dto.ChannelSort;
import com.advertmarket.marketplace.channel.mapper.CategoryDtoMapper;
import com.advertmarket.marketplace.channel.mapper.ChannelListItemMapper;
import com.advertmarket.marketplace.channel.repository.JooqCategoryRepository;
import com.advertmarket.marketplace.channel.search.ParadeDbChannelSearch;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.pagination.CursorPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultExecuteListener;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * Integration test for ParadeDbChannelSearch filter-based search.
 *
 * <p>BM25 text search ({@code channels @@@}) requires ParadeDB image
 * and is not tested here. Only filter/sort/pagination logic is covered.
 */
@DisplayName("ParadeDbChannelSearch — filter-based search integration")
class ChannelSearchIntegrationTest {

    private static final long USER_ID = 1L;

    private static DSLContext dsl;
    private ParadeDbChannelSearch search;

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
        search = new ParadeDbChannelSearch(
                dsl,
                categoryRepo,
                Mappers.getMapper(ChannelListItemMapper.class));
        DatabaseSupport.cleanAllTables(dsl);
        TestDataFactory.upsertUser(dsl, USER_ID);
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
    @DisplayName("Should paginate relevance without query (fallback order must be cursor-safe)")
    void shouldPaginateRelevanceWithoutQuery() {
        for (int i = 1; i <= 5; i++) {
            insertChannel(-i, "Chan " + i, "tech", i * 1000, null, true);
        }

        CursorPage<ChannelListItem> page1 = search.search(criteria(
                null, null, null, null, null,
                ChannelSort.RELEVANCE, null, 2));
        CursorPage<ChannelListItem> page2 = search.search(criteria(
                null, null, null, null, null,
                ChannelSort.RELEVANCE, page1.nextCursor(), 2));

        assertThat(page1.items()).hasSize(2);
        assertThat(page1.nextCursor()).isNotNull();
        assertThat(page2.items()).hasSize(2);

        var page1Ids = page1.items().stream()
                .map(ChannelListItem::id)
                .collect(java.util.stream.Collectors.toSet());
        var page2Ids = page2.items().stream()
                .map(ChannelListItem::id)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(page2Ids).doesNotContainAnyElementsOf(page1Ids);
    }

    @Test
    @DisplayName("Should paginate relevance with BM25 query using score+id keyset")
    void shouldPaginateRelevanceWithQuery() {
        insertChannel(1L, "alpha alpha alpha alpha", "tech", 1000, null, true);
        insertChannel(2L, "alpha alpha", "tech", 1000, null, true);
        insertChannel(3L, "alpha", "tech", 1000, null, true);

        CursorPage<ChannelListItem> page1 = search.search(criteriaWithQuery(
                "alpha", ChannelSort.RELEVANCE, null, 2));
        CursorPage<ChannelListItem> page2 = search.search(criteriaWithQuery(
                "alpha", ChannelSort.RELEVANCE, page1.nextCursor(), 2));

        assertThat(page1.items()).hasSize(2);
        assertThat(page1.nextCursor()).isNotNull();
        assertThat(page2.items()).hasSize(1);

        Set<Long> distinctIds = new java.util.HashSet<>();
        page1.items().forEach(it -> distinctIds.add(it.id()));
        page2.items().forEach(it -> distinctIds.add(it.id()));
        assertThat(distinctIds).hasSize(3);
    }

    @Test
    @DisplayName("Search should not execute N+1 queries for category mapping")
    void search_shouldNotExecuteNplusOneQueriesForCategories() throws Exception {
        for (int i = 1; i <= 5; i++) {
            insertChannel(-i, "Chan " + i, "tech", i * 1000, null, true);
        }

        AtomicInteger statements = new AtomicInteger();
        var listener = new DefaultExecuteListener() {
            @Override
            public void executeStart(ExecuteContext ctx) {
                statements.incrementAndGet();
            }
        };

        try (Connection conn = DriverManager.getConnection(
                SharedContainers.pgJdbcUrl(),
                SharedContainers.pgUsername(),
                SharedContainers.pgPassword())) {
            var dslWithListener = DSL.using(conn, org.jooq.SQLDialect.POSTGRES);
            dslWithListener.configuration().set(
                    new DefaultExecuteListenerProvider(listener));

            var jsonFacade = new JsonFacade(new ObjectMapper());
            var categoryRepo = new JooqCategoryRepository(
                    dslWithListener,
                    jsonFacade,
                    Mappers.getMapper(CategoryDtoMapper.class));
            var searchWithListener = new ParadeDbChannelSearch(
                    dslWithListener,
                    categoryRepo,
                    Mappers.getMapper(ChannelListItemMapper.class));

            statements.set(0);
            CursorPage<ChannelListItem> page = searchWithListener.search(criteria(
                    null, null, null, null, null,
                    ChannelSort.SUBSCRIBERS_DESC, null, 5));

            assertThat(page.items()).hasSize(5);
            assertThat(statements.get()).isLessThanOrEqualTo(4);
        }
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

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static ChannelSearchCriteria criteria(
            String category, Integer minSub, Integer maxSub,
            Long minPrice, Long maxPrice, ChannelSort sort,
            String cursor, int limit) {
        return new ChannelSearchCriteria(
                category, minSub, maxSub, minPrice, maxPrice,
                null, null, null, sort, cursor, limit);
    }

    private static ChannelSearchCriteria criteriaWithQuery(
            String query, ChannelSort sort, String cursor, int limit) {
        return new ChannelSearchCriteria(
                null, null, null, null, null,
                null, null, query, sort, cursor, limit);
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
}

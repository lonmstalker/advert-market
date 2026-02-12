package com.advertmarket.integration.marketplace;

import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.marketplace.adapter.JooqChannelRepository;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.NewChannel;
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
        repository = new JooqChannelRepository(dsl);
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
        assertThat(found.get().category()).isEqualTo("tech");
    }

    @Test
    @DisplayName("Should return empty for non-existent find")
    void shouldReturnEmptyForNonExistentFind() {
        Optional<ChannelResponse> found =
                repository.findByTelegramId(CHANNEL_ID);

        assertThat(found).isEmpty();
    }

    private static NewChannel testChannel() {
        return new NewChannel(
                CHANNEL_ID, CHANNEL_TITLE, CHANNEL_USERNAME,
                "Test description", 5000, "tech",
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

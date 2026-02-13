package com.advertmarket.integration.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.identity.adapter.JooqUserRepository;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.shared.model.UserId;
import java.util.List;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for JooqUserRepository with real PostgreSQL.
 */
@Testcontainers
@DisplayName("JooqUserRepository — PostgreSQL integration")
class JooqUserRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName
                    .parse("paradedb/paradedb:latest")
                    .asCompatibleSubstituteFor("postgres"));

    private static DSLContext dsl;
    private JooqUserRepository repository;

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
        repository = new JooqUserRepository(dsl);
        dsl.deleteFrom(com.advertmarket.db.generated.tables.Users.USERS)
                .execute();
    }

    @Test
    @DisplayName("Should insert new user on first upsert")
    void shouldInsertNewUser() {
        TelegramUserData data = new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en");

        boolean isOperator = repository.upsert(data);

        assertThat(isOperator).isFalse();

        UserProfile profile = repository.findById(new UserId(42L));
        assertThat(profile).isNotNull();
        assertThat(profile.username()).isEqualTo("johndoe");
        assertThat(profile.displayName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should update existing user on second upsert")
    void shouldUpdateExistingUser() {
        TelegramUserData original = new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en");
        repository.upsert(original);

        TelegramUserData updated = new TelegramUserData(
                42L, "Johnny", "D", "johnny", "ru");
        repository.upsert(updated);

        UserProfile profile = repository.findById(new UserId(42L));
        assertThat(profile).isNotNull();
        assertThat(profile.username()).isEqualTo("johnny");
        assertThat(profile.displayName()).isEqualTo("Johnny D");
    }

    @Test
    @DisplayName("Should complete onboarding with interests")
    void shouldCompleteOnboarding() {
        repository.upsert(new TelegramUserData(
                42L, "John", null, null, "en"));

        repository.completeOnboarding(
                new UserId(42L),
                List.of("tech", "gaming"));

        UserProfile profile = repository.findById(new UserId(42L));
        assertThat(profile).isNotNull();
        assertThat(profile.onboardingCompleted()).isTrue();
        assertThat(profile.interests())
                .containsExactly("tech", "gaming");
    }

    @Test
    @DisplayName("Should soft-delete user and clear PII")
    void shouldSoftDelete() {
        repository.upsert(new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en"));

        repository.softDelete(new UserId(42L));

        UserProfile profile = repository.findById(new UserId(42L));
        assertThat(profile).isNull();
    }

    @Test
    @DisplayName("Should reactivate soft-deleted user on new login")
    void shouldReactivateOnNewLogin() {
        repository.upsert(new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en"));
        repository.softDelete(new UserId(42L));

        assertThat(repository.findById(new UserId(42L))).isNull();

        repository.upsert(new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en"));

        UserProfile profile = repository.findById(new UserId(42L));
        assertThat(profile).isNotNull();
        assertThat(profile.username()).isEqualTo("johndoe");
    }

    @Test
    @DisplayName("Should return null for non-existent user")
    void shouldReturnNullForNonExistent() {
        UserProfile profile = repository.findById(new UserId(999L));
        assertThat(profile).isNull();
    }
}

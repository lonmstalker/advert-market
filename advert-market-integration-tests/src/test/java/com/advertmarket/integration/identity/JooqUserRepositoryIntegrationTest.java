package com.advertmarket.integration.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.identity.adapter.JooqUserRepository;
import com.advertmarket.identity.api.dto.CurrencyMode;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.config.LocaleCurrencyProperties;
import com.advertmarket.identity.mapper.UserProfileMapper;
import com.advertmarket.identity.service.LocaleCurrencyResolver;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.UserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * Integration test for JooqUserRepository with real PostgreSQL.
 */
@DisplayName("JooqUserRepository — PostgreSQL integration")
class JooqUserRepositoryIntegrationTest {

    private static DSLContext dsl;
    private JooqUserRepository repository;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
        dsl = DatabaseSupport.dsl();
    }

    @BeforeEach
    void setUp() {
        LocaleCurrencyProperties localeCurrencyProperties =
                new LocaleCurrencyProperties(
                        "USD",
                        Map.of(
                                "ru", "RUB",
                                "en", "USD"));

        repository = new JooqUserRepository(
                dsl,
                new JsonFacade(new ObjectMapper()
                        .findAndRegisterModules()),
                Mappers.getMapper(UserProfileMapper.class),
                new LocaleCurrencyResolver(localeCurrencyProperties));
        DatabaseSupport.cleanAllTables(dsl);
    }

    @Test
    @DisplayName("Should insert new user on first upsert")
    void shouldInsertNewUser() {
        TelegramUserData data = new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en");

        boolean isOperator = repository.upsert(data);

        assertThat(isOperator).isFalse();

        assertThat(repository.findById(new UserId(42L)))
                .isPresent()
                .hasValueSatisfying(profile -> {
                    assertThat(profile.username()).isEqualTo("johndoe");
                    assertThat(profile.displayName()).isEqualTo("John Doe");
                });
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

        assertThat(repository.findById(new UserId(42L)))
                .isPresent()
                .hasValueSatisfying(profile -> {
                    assertThat(profile.username()).isEqualTo("johnny");
                    assertThat(profile.displayName()).isEqualTo("Johnny D");
                    assertThat(profile.languageCode()).isEqualTo("en");
                });
    }

    @Test
    @DisplayName("Should derive display currency from language in AUTO mode")
    void shouldDeriveDisplayCurrencyFromLanguageInAutoMode() {
        UserId userId = new UserId(42L);
        repository.upsert(new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en"));
        repository.updateCurrencyMode(userId, CurrencyMode.AUTO);

        repository.updateLanguage(userId, "ru");

        assertThat(repository.findById(userId))
                .isPresent()
                .hasValueSatisfying(profile -> {
                    assertThat(profile.currencyMode()).isEqualTo(CurrencyMode.AUTO);
                    assertThat(profile.displayCurrency()).isEqualTo("RUB");
                });
    }

    @Test
    @DisplayName("Should complete onboarding with interests")
    void shouldCompleteOnboarding() {
        repository.upsert(new TelegramUserData(
                42L, "John", null, null, "en"));

        repository.completeOnboarding(
                new UserId(42L),
                List.of("tech", "gaming"));

        assertThat(repository.findById(new UserId(42L)))
                .isPresent()
                .hasValueSatisfying(profile -> {
                    assertThat(profile.onboardingCompleted()).isTrue();
                    assertThat(profile.interests())
                            .containsExactly("tech", "gaming");
                });
    }

    @Test
    @DisplayName("Should soft-delete user and clear PII")
    void shouldSoftDelete() {
        repository.upsert(new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en"));

        repository.softDelete(new UserId(42L));

        assertThat(repository.findById(new UserId(42L))).isEmpty();
    }

    @Test
    @DisplayName("Should reactivate soft-deleted user on new login")
    void shouldReactivateOnNewLogin() {
        repository.upsert(new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en"));
        repository.softDelete(new UserId(42L));

        assertThat(repository.findById(new UserId(42L))).isEmpty();

        repository.upsert(new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en"));

        assertThat(repository.findById(new UserId(42L)))
                .isPresent()
                .hasValueSatisfying(profile ->
                        assertThat(profile.username()).isEqualTo("johndoe"));
    }

    @Test
    @DisplayName("Should return empty for non-existent user")
    void shouldReturnEmptyForNonExistent() {
        assertThat(repository.findById(new UserId(999L))).isEmpty();
    }
}

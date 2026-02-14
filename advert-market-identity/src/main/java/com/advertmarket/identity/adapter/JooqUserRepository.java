package com.advertmarket.identity.adapter;

import static com.advertmarket.db.generated.tables.Users.USERS;

import com.advertmarket.identity.api.dto.NotificationSettings;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.UserId;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * Persists user data using jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqUserRepository implements UserRepository {

    private final DSLContext dsl;
    private final JsonFacade jsonFacade;

    @Override
    public boolean upsert(@NonNull TelegramUserData data) {
        OffsetDateTime now = OffsetDateTime.now();
        String lang = data.languageCode() != null
                ? data.languageCode() : "ru";
        String defaultCurrency = defaultCurrencyForLanguage(lang);

        Record result = dsl.insertInto(USERS)
                .set(USERS.ID, data.id())
                .set(USERS.FIRST_NAME, data.firstName())
                .set(USERS.LAST_NAME, data.lastName())
                .set(USERS.USERNAME, data.username())
                .set(USERS.LANGUAGE_CODE, lang)
                .set(USERS.DISPLAY_CURRENCY, defaultCurrency)
                .set(USERS.UPDATED_AT, now)
                .onConflict(USERS.ID)
                .doUpdate()
                .set(USERS.FIRST_NAME, data.firstName())
                .set(USERS.LAST_NAME, data.lastName())
                .set(USERS.USERNAME, data.username())
                .set(USERS.LANGUAGE_CODE, lang)
                .set(USERS.IS_DELETED, false)
                .setNull(USERS.DELETED_AT)
                .set(USERS.UPDATED_AT, now)
                .returning(USERS.IS_OPERATOR)
                .fetchOne();

        return result != null
                && Boolean.TRUE.equals(
                result.get(USERS.IS_OPERATOR));
    }

    @Override
    public @NonNull Optional<UserProfile> findById(@NonNull UserId userId) {
        Record record = dsl.select()
                .from(USERS)
                .where(USERS.ID.eq(userId.value()))
                .and(USERS.IS_DELETED.isFalse()
                        .or(USERS.IS_DELETED.isNull()))
                .fetchOne();

        return Optional.ofNullable(
                record != null ? mapToProfile(record) : null);
    }

    @Override
    public void softDelete(@NonNull UserId userId) {
        dsl.update(USERS)
                .set(USERS.IS_DELETED, true)
                .set(USERS.DELETED_AT, OffsetDateTime.now())
                .set(USERS.FIRST_NAME, "Deleted")
                .setNull(USERS.LAST_NAME)
                .setNull(USERS.USERNAME)
                .set(USERS.UPDATED_AT, OffsetDateTime.now())
                .where(USERS.ID.eq(userId.value()))
                .execute();
    }

    @Override
    public void completeOnboarding(@NonNull UserId userId,
            @NonNull List<String> interests) {
        dsl.update(USERS)
                .set(USERS.ONBOARDING_COMPLETED, true)
                .set(USERS.INTERESTS,
                        interests.toArray(String[]::new))
                .set(USERS.UPDATED_AT,
                        OffsetDateTime.now())
                .where(USERS.ID.eq(userId.value()))
                .execute();
    }

    @Override
    public void updateLanguage(@NonNull UserId userId,
            @NonNull String languageCode) {
        dsl.update(USERS)
                .set(USERS.LANGUAGE_CODE, languageCode)
                .set(USERS.UPDATED_AT, OffsetDateTime.now())
                .where(USERS.ID.eq(userId.value()))
                .execute();
    }

    @Override
    public void updateDisplayCurrency(@NonNull UserId userId,
            @NonNull String currency) {
        dsl.update(USERS)
                .set(USERS.DISPLAY_CURRENCY, currency)
                .set(USERS.UPDATED_AT, OffsetDateTime.now())
                .where(USERS.ID.eq(userId.value()))
                .execute();
    }

    @Override
    public void updateNotificationSettings(@NonNull UserId userId,
            @NonNull NotificationSettings settings) {
        dsl.update(USERS)
                .set(USERS.NOTIFICATION_SETTINGS,
                        JSON.json(jsonFacade.toJson(settings)))
                .set(USERS.UPDATED_AT, OffsetDateTime.now())
                .where(USERS.ID.eq(userId.value()))
                .execute();
    }

    private UserProfile mapToProfile(Record record) {
        long id = record.get(USERS.ID);
        String firstName = record.get(USERS.FIRST_NAME);
        String lastName = record.get(USERS.LAST_NAME);
        String username = record.get(USERS.USERNAME);
        String languageCode = record.get(USERS.LANGUAGE_CODE);
        String displayCurrency = record.get(USERS.DISPLAY_CURRENCY);
        JSON notifJson = record.get(USERS.NOTIFICATION_SETTINGS);
        Boolean onboardingCompleted = record.get(
                USERS.ONBOARDING_COMPLETED);
        String[] interests = record.get(USERS.INTERESTS);
        OffsetDateTime createdAt = record.get(USERS.CREATED_AT);

        String displayName = lastName != null
                && !lastName.isBlank()
                ? firstName + " " + lastName
                : firstName != null ? firstName : "";

        NotificationSettings notificationSettings =
                notifJson != null && notifJson.data() != null
                        ? jsonFacade.fromJson(notifJson.data(),
                        NotificationSettings.class)
                        : NotificationSettings.defaults();

        return new UserProfile(
                id,
                username != null ? username : "",
                displayName,
                languageCode != null ? languageCode : "ru",
                displayCurrency != null ? displayCurrency : "USD",
                notificationSettings,
                Boolean.TRUE.equals(onboardingCompleted),
                interests != null
                        ? Arrays.asList(interests)
                        : List.of(),
                createdAt != null
                        ? createdAt.toInstant()
                        : Instant.now());
    }

    private static String defaultCurrencyForLanguage(String languageCode) {
        return "ru".equals(languageCode) ? "RUB" : "USD";
    }
}

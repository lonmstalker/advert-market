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
        return dsl.select(
                        USERS.ID,
                        USERS.USERNAME,
                        USERS.FIRST_NAME,
                        USERS.LAST_NAME,
                        USERS.LANGUAGE_CODE,
                        USERS.DISPLAY_CURRENCY,
                        USERS.NOTIFICATION_SETTINGS,
                        USERS.ONBOARDING_COMPLETED,
                        USERS.INTERESTS,
                        USERS.CREATED_AT)
                .from(USERS)
                .where(USERS.ID.eq(userId.value()))
                .and(USERS.IS_DELETED.isFalse()
                        .or(USERS.IS_DELETED.isNull()))
                .fetchOptional()
                .map(this::mapToProfile);
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
        return new UserProfile(
                id,
                orEmpty(record.get(USERS.USERNAME)),
                displayName(firstName, lastName),
                defaultLanguage(record.get(USERS.LANGUAGE_CODE)),
                defaultCurrency(record.get(USERS.DISPLAY_CURRENCY)),
                notificationSettings(record.get(USERS.NOTIFICATION_SETTINGS)),
                Boolean.TRUE.equals(record.get(USERS.ONBOARDING_COMPLETED)),
                interests(record.get(USERS.INTERESTS)),
                createdAt(record.get(USERS.CREATED_AT)));
    }

    private static String defaultCurrencyForLanguage(String languageCode) {
        return "ru".equals(languageCode) ? "RUB" : "USD";
    }

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }

    private static String defaultLanguage(String languageCode) {
        return languageCode != null ? languageCode : "ru";
    }

    private static String defaultCurrency(String currency) {
        return currency != null ? currency : "USD";
    }

    private static String displayName(String firstName, String lastName) {
        if (lastName != null && !lastName.isBlank()) {
            return (firstName != null ? firstName : "") + " " + lastName;
        }
        return firstName != null ? firstName : "";
    }

    private NotificationSettings notificationSettings(JSON notifJson) {
        if (notifJson != null && notifJson.data() != null) {
            return jsonFacade.fromJson(notifJson.data(),
                    NotificationSettings.class);
        }
        return NotificationSettings.defaults();
    }

    private static List<String> interests(String[] interests) {
        return interests != null ? Arrays.asList(interests) : List.of();
    }

    private static Instant createdAt(OffsetDateTime createdAt) {
        return createdAt != null ? createdAt.toInstant() : Instant.now();
    }
}

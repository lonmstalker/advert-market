package com.advertmarket.identity.adapter;

import static com.advertmarket.db.generated.tables.Users.USERS;

import com.advertmarket.identity.api.dto.NotificationSettings;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.identity.mapper.UserProfileMapper;
import com.advertmarket.identity.mapper.UserProfileRow;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.UserId;
import java.time.OffsetDateTime;
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
    private final UserProfileMapper userProfileMapper;

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
                        USERS.ID.as("id"),
                        USERS.USERNAME.as("username"),
                        USERS.FIRST_NAME.as("firstName"),
                        USERS.LAST_NAME.as("lastName"),
                        USERS.LANGUAGE_CODE.as("languageCode"),
                        USERS.DISPLAY_CURRENCY.as("displayCurrency"),
                        USERS.NOTIFICATION_SETTINGS.as("notificationSettings"),
                        USERS.ONBOARDING_COMPLETED.as("onboardingCompleted"),
                        USERS.INTERESTS.as("interests"),
                        USERS.CREATED_AT.as("createdAt"))
                .from(USERS)
                .where(USERS.ID.eq(userId.value()))
                .and(USERS.IS_DELETED.isFalse()
                        .or(USERS.IS_DELETED.isNull()))
                .fetchOptionalInto(UserProfileRow.class)
                .map(row -> userProfileMapper.toProfile(row, jsonFacade));
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

    private static String defaultCurrencyForLanguage(String languageCode) {
        return "ru".equals(languageCode) ? "RUB" : "USD";
    }
}

package com.advertmarket.identity.adapter;

import static com.advertmarket.db.generated.tables.Users.USERS;

import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.model.UserId;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * Persists user data using jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqUserRepository implements UserRepository {

    private final DSLContext dsl;

    @Override
    public boolean upsert(@NonNull TelegramUserData data) {
        OffsetDateTime now = OffsetDateTime.now();
        String lang = data.languageCode() != null
                ? data.languageCode() : "ru";

        Record result = dsl.insertInto(USERS)
                .set(USERS.ID, data.id())
                .set(USERS.FIRST_NAME, data.firstName())
                .set(USERS.LAST_NAME, data.lastName())
                .set(USERS.USERNAME, data.username())
                .set(USERS.LANGUAGE_CODE, lang)
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
    @Nullable
    public UserProfile findById(@NonNull UserId userId) {
        Record record = dsl.select()
                .from(USERS)
                .where(USERS.ID.eq(userId.value()))
                .and(USERS.IS_DELETED.isFalse()
                        .or(USERS.IS_DELETED.isNull()))
                .fetchOne();

        if (record == null) {
            return null;
        }

        return mapToProfile(record);
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

    private static UserProfile mapToProfile(Record record) {
        long id = record.get(USERS.ID);
        String firstName = record.get(USERS.FIRST_NAME);
        String lastName = record.get(USERS.LAST_NAME);
        String username = record.get(USERS.USERNAME);
        String languageCode = record.get(USERS.LANGUAGE_CODE);
        Boolean onboardingCompleted = record.get(
                USERS.ONBOARDING_COMPLETED);
        String[] interests = record.get(USERS.INTERESTS);
        OffsetDateTime createdAt = record.get(USERS.CREATED_AT);

        String displayName = lastName != null
                && !lastName.isBlank()
                ? firstName + " " + lastName
                : firstName != null ? firstName : "";

        return new UserProfile(
                id,
                username != null ? username : "",
                displayName,
                languageCode != null ? languageCode : "ru",
                Boolean.TRUE.equals(onboardingCompleted),
                interests != null
                        ? Arrays.asList(interests)
                        : List.of(),
                createdAt != null
                        ? createdAt.toInstant()
                        : Instant.now());
    }
}

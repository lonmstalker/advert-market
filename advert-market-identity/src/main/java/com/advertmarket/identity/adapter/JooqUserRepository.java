package com.advertmarket.identity.adapter;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.val;

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
        Record result = dsl.insertInto(table("users"))
                .set(field("id"), data.id())
                .set(field("first_name"), data.firstName())
                .set(field("last_name"), data.lastName())
                .set(field("username"), data.username())
                .set(field("language_code"),
                        data.languageCode() != null
                                ? data.languageCode() : "ru")
                .set(field("updated_at"), now)
                .onConflict(field("id"))
                .doUpdate()
                .set(field("first_name"), data.firstName())
                .set(field("last_name"), data.lastName())
                .set(field("username"), data.username())
                .set(field("language_code"),
                        data.languageCode() != null
                                ? data.languageCode() : "ru")
                .set(field("updated_at"), now)
                .returning(field("is_operator"))
                .fetchOne();

        return result != null
                && Boolean.TRUE.equals(
                result.get(field("is_operator"), Boolean.class));
    }

    @Override
    @Nullable
    public UserProfile findById(@NonNull UserId userId) {
        Record record = dsl.select()
                .from(table("users"))
                .where(field("id").eq(userId.value()))
                .fetchOne();

        if (record == null) {
            return null;
        }

        return mapToProfile(record);
    }

    @Override
    public void completeOnboarding(@NonNull UserId userId,
            @NonNull List<String> interests) {
        dsl.update(table("users"))
                .set(field("onboarding_completed"), true)
                .set(field("interests"),
                        interests.toArray(String[]::new))
                .set(field("updated_at"),
                        OffsetDateTime.now())
                .where(field("id").eq(userId.value()))
                .execute();
    }

    private static UserProfile mapToProfile(Record record) {
        long id = record.get(field("id"), Long.class);
        String firstName = record.get(
                field("first_name"), String.class);
        String lastName = record.get(
                field("last_name"), String.class);
        String username = record.get(
                field("username"), String.class);
        String languageCode = record.get(
                field("language_code"), String.class);
        Boolean onboardingCompleted = record.get(
                field("onboarding_completed"), Boolean.class);
        String[] interests = record.get(
                field("interests"), String[].class);
        OffsetDateTime createdAt = record.get(
                field("created_at"), OffsetDateTime.class);

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

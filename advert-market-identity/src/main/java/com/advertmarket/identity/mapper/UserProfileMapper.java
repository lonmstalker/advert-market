package com.advertmarket.identity.mapper;

import com.advertmarket.identity.api.dto.NotificationSettings;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.dto.CurrencyMode;
import com.advertmarket.shared.json.JsonFacade;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.jooq.JSONB;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link UserProfile}.
 */
@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    /** Maps DB projection row into API profile DTO. */
    @Mapping(target = "username",
            expression = "java(orEmpty(row.username()))")
    @Mapping(target = "displayName",
            expression = "java(displayName(row.firstName(), row.lastName()))")
    @Mapping(target = "languageCode",
            expression = "java(defaultLanguage(row.languageCode()))")
    @Mapping(target = "displayCurrency",
            expression = "java(defaultCurrency(row.displayCurrency()))")
    @Mapping(target = "currencyMode",
            expression = "java(defaultCurrencyMode(row.currencyMode()))")
    @Mapping(target = "notificationSettings",
            source = "notificationSettings")
    @Mapping(target = "onboardingCompleted",
            expression = "java(onboardingCompleted(row.onboardingCompleted()))")
    @Mapping(target = "interests", source = "interests")
    @Mapping(target = "createdAt", source = "createdAt")
    UserProfile toProfile(UserProfileRow row, @Context JsonFacade json);

    /**
     * Returns empty string for {@code null} values.
     */
    default String orEmpty(String value) {
        return value != null ? value : "";
    }

    /**
     * Returns language code or {@code "ru"} when absent.
     */
    default String defaultLanguage(String languageCode) {
        return languageCode != null ? languageCode : "ru";
    }

    /**
     * Returns currency code or {@code "USD"} when absent.
     */
    default String defaultCurrency(String currency) {
        return currency != null ? currency : "USD";
    }

    /**
     * Returns currency mode or {@code AUTO} when absent/invalid.
     */
    default CurrencyMode defaultCurrencyMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return CurrencyMode.AUTO;
        }
        try {
            return CurrencyMode.valueOf(mode);
        } catch (IllegalArgumentException ex) {
            return CurrencyMode.AUTO;
        }
    }

    /**
     * Builds display name from first/last name parts.
     */
    default String displayName(String firstName, String lastName) {
        if (lastName != null && !lastName.isBlank()) {
            return (firstName != null ? firstName : "") + " " + lastName;
        }
        return firstName != null ? firstName : "";
    }

    /**
     * Parses notification settings JSON or returns defaults when missing.
     */
    default NotificationSettings notificationSettings(
            JSONB notifJson,
            @Context JsonFacade jsonFacade) {
        if (notifJson != null && notifJson.data() != null) {
            return jsonFacade.fromJson(notifJson.data(),
                    NotificationSettings.class);
        }
        return NotificationSettings.defaults();
    }

    /**
     * Converts nullable onboarding flag to boolean.
     */
    default boolean onboardingCompleted(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    /**
     * Converts optional interests array to list.
     */
    default List<String> interests(String[] interests) {
        return interests != null ? Arrays.asList(interests) : List.of();
    }

    /**
     * Converts optional creation timestamp to {@link Instant}.
     * Defaults to {@link Instant#now()} when missing.
     */
    default Instant createdAt(OffsetDateTime createdAt) {
        return createdAt != null ? createdAt.toInstant() : Instant.now();
    }
}

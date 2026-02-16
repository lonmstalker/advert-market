package com.advertmarket.identity.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Locale-to-currency mapping properties.
 */
@ConfigurationProperties(prefix = "app.locale-currency")
@Validated
@PropertyGroupDoc(
        displayName = "Locale Currency Mapping",
        description = "Server-driven mapping from language code to effective display currency",
        category = "Identity"
)
public record LocaleCurrencyProperties(
        @PropertyDoc(
                description = "Currency code used when language mapping is missing",
                required = Requirement.REQUIRED
        )
        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        @NonNull String fallbackCurrency,

        @PropertyDoc(
                description = "Map of language code (e.g. ru, en) to ISO 4217 currency code",
                required = Requirement.REQUIRED
        )
        @NonNull Map<String, String> languageMap
) {
    /** Compact constructor that makes the language map immutable. */
    public LocaleCurrencyProperties {
        languageMap = Map.copyOf(languageMap);
    }
}

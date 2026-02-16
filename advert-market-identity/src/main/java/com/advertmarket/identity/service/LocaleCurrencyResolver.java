package com.advertmarket.identity.service;

import com.advertmarket.identity.config.LocaleCurrencyProperties;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

/**
 * Resolves effective display currency from user language.
 */
@Component
@RequiredArgsConstructor
public class LocaleCurrencyResolver {

    private final LocaleCurrencyProperties properties;

    /**
     * Resolves currency for a language code with fallback.
     *
     * @param languageCode user language code, e.g. ru, en-US
     * @return ISO 4217 uppercase currency code
     */
    public @NonNull String resolve(@Nullable String languageCode) {
        Map<String, String> mapping = properties.languageMap();
        if (languageCode == null || languageCode.isBlank()) {
            return properties.fallbackCurrency();
        }

        String normalized = languageCode.trim().toLowerCase(Locale.ROOT);
        String byFullCode = mapping.get(normalized);
        if (byFullCode != null && !byFullCode.isBlank()) {
            return byFullCode.toUpperCase(Locale.ROOT);
        }

        int separatorIndex = normalized.indexOf('-');
        String languageRoot = separatorIndex > 0
                ? normalized.substring(0, separatorIndex)
                : normalized;
        String byRoot = mapping.get(languageRoot);
        if (byRoot != null && !byRoot.isBlank()) {
            return byRoot.toUpperCase(Locale.ROOT);
        }

        return properties.fallbackCurrency();
    }
}

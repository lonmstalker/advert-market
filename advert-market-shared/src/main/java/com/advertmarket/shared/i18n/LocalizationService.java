package com.advertmarket.shared.i18n;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

/**
 * Thin wrapper over Spring {@link MessageSource} for convenience.
 */
@RequiredArgsConstructor
public class LocalizationService {

    private static final Locale DEFAULT_LOCALE = Locale.of("ru");

    private final MessageSource messageSource;

    /**
     * Resolves a message by key and locale.
     *
     * @param key    the message key
     * @param locale the target locale
     * @param args   optional message arguments
     * @return the resolved message, or the key itself on miss
     */
    @NonNull
    public String msg(@NonNull String key, @NonNull Locale locale,
            Object @NonNull ... args) {
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException e) {
            return key;
        }
    }

    /**
     * Resolves a message by key and language code string.
     *
     * @param key      the message key
     * @param langCode language code (e.g. "ru", "en"), or null
     * @param args     optional message arguments
     * @return the resolved message, or the key itself on miss
     */
    @NonNull
    public String msg(@NonNull String key, @Nullable String langCode,
            Object @NonNull ... args) {
        Locale locale;
        try {
            locale = StringUtils.isNotBlank(langCode)
                    ? Locale.of(langCode)
                    : DEFAULT_LOCALE;
        } catch (IllegalArgumentException e) {
            locale = DEFAULT_LOCALE;
        }
        return msg(key, locale, args);
    }
}

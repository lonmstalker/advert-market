package com.advertmarket.shared.i18n;

import java.util.Locale;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over Spring {@link MessageSource} for convenience.
 */
@Component
public class LocalizationService {

    private static final Locale DEFAULT_LOCALE = Locale.of("ru");

    private final MessageSource messageSource;

    /** Creates the service with the given message source. */
    public LocalizationService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

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
            Object... args) {
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
    public String msg(@NonNull String key, String langCode,
            Object... args) {
        Locale locale = langCode != null && !langCode.isBlank()
                ? Locale.of(langCode) : DEFAULT_LOCALE;
        return msg(key, locale, args);
    }
}

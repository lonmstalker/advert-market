package com.advertmarket.app.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.i18n.LocalizationService;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

@DisplayName("LocalizationService resource bundle fallback")
class LocalizationServiceResourceBundleFallbackTest {

    @Test
    @DisplayName("Falls back to ResourceBundle when MessageSource misses")
    void fallsBackToResourceBundleWhenMessageSourceMisses() {
        MessageSource missingMessageSource = new MessageSource() {
            @Override
            public String getMessage(String code, Object[] args,
                    String defaultMessage, Locale locale) {
                throw new NoSuchMessageException(code, locale);
            }

            @Override
            public String getMessage(String code, Object[] args,
                    Locale locale) throws NoSuchMessageException {
                throw new NoSuchMessageException(code, locale);
            }

            @Override
            public String getMessage(MessageSourceResolvable resolvable,
                    Locale locale) throws NoSuchMessageException {
                throw new NoSuchMessageException(
                        resolvable.getCodes() != null
                                ? resolvable.getCodes()[0]
                                : "<unknown>",
                        locale);
            }
        };

        LocalizationService i18n =
                new LocalizationService(missingMessageSource);

        String welcome = i18n.msg("bot.welcome", Locale.of("ru"));
        assertThat(welcome).contains("Advert Market");

        String selected = i18n.msg("bot.language.selected", "ru",
                "RU \u0420\u0443\u0441\u0441\u043a\u0438\u0439");
        assertThat(selected)
                .contains("RU")
                .contains("\u2714");
    }
}


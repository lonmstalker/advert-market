package com.advertmarket.shared.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

@DisplayName("LocalizationService — message resolution and locale handling")
class LocalizationServiceTest {

    private final MessageSource messageSource =
            mock(MessageSource.class);
    private final LocalizationService service =
            new LocalizationService(messageSource);

    @Test
    @DisplayName("Resolves message by key and language code")
    void msg_resolvesMessage() {
        when(messageSource.getMessage(
                eq("bot.welcome"), any(), eq(Locale.of("ru"))))
                .thenReturn("Welcome!");
        assertThat(service.msg("bot.welcome", "ru"))
                .isEqualTo("Welcome!");
    }

    @Test
    @DisplayName("Resolves message with arguments")
    void msg_resolvesWithArgs() {
        Object[] args = {"Test"};
        when(messageSource.getMessage(
                eq("bot.selected"), eq(args),
                eq(Locale.of("en"))))
                .thenReturn("Selected: Test");
        assertThat(service.msg("bot.selected", "en", "Test"))
                .isEqualTo("Selected: Test");
    }

    @Test
    @DisplayName("Falls back to key when message not found")
    void msg_fallsBackToKeyOnMiss() {
        when(messageSource.getMessage(
                eq("missing.key"), any(), eq(Locale.of("ru"))))
                .thenThrow(new NoSuchMessageException("nope"));
        assertThat(service.msg("missing.key", "ru"))
                .isEqualTo("missing.key");
    }

    @Test
    @DisplayName("Defaults to Russian for null language code")
    void msg_defaultsToRussianForNullLang() {
        when(messageSource.getMessage(
                eq("bot.test"), any(), eq(Locale.of("ru"))))
                .thenReturn("Test RU");
        assertThat(service.msg("bot.test", (String) null))
                .isEqualTo("Test RU");
    }

    @Test
    @DisplayName("Defaults to Russian for blank language code")
    void msg_defaultsToRussianForBlankLang() {
        when(messageSource.getMessage(
                eq("bot.test"), any(), eq(Locale.of("ru"))))
                .thenReturn("Test RU");
        assertThat(service.msg("bot.test", "  "))
                .isEqualTo("Test RU");
    }
}

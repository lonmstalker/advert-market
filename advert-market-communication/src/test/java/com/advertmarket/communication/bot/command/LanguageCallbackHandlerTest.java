package com.advertmarket.communication.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.shared.i18n.LocalizationService;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LanguageCallbackHandler")
class LanguageCallbackHandlerTest {

    private final LocalizationService i18n =
            mock(LocalizationService.class);
    private final TelegramSender sender =
            mock(TelegramSender.class);
    private final LanguageCallbackHandler handler =
            new LanguageCallbackHandler(i18n);

    @Test
    @DisplayName("Prefix returns 'lang:'")
    void prefix_returnsLang() {
        assertThat(handler.prefix()).isEqualTo("lang:");
    }

    @Test
    @DisplayName("Extracts language code from callback data")
    void handle_extractsLangCode() throws Exception {
        when(i18n.msg(eq("bot.language.selected"),
                eq("en"), eq("English")))
                .thenReturn("Language: English");

        var ctx = createCallbackContext(1L, "lang:en", "en");

        handler.handle(ctx, sender);

        verify(i18n).msg("bot.language.selected",
                "en", "English");
    }

    @Test
    @DisplayName("Sends callback answer with selected language")
    void handle_sendsCallbackAnswer() throws Exception {
        when(i18n.msg(eq("bot.language.selected"),
                eq("ru"), eq("Русский")))
                .thenReturn("Язык: Русский");

        var ctx = createCallbackContext(1L, "lang:ru", "ru");

        handler.handle(ctx, sender);

        verify(sender).execute(any(AnswerCallbackQuery.class));
    }

    @Test
    @DisplayName("Falls back to Russian when language code is null")
    void handle_fallbackToRussian() throws Exception {
        when(i18n.msg(eq("bot.language.selected"),
                eq("ru"), eq("English")))
                .thenReturn("Language: English");

        var ctx = createCallbackContext(1L, "lang:en", null);

        handler.handle(ctx, sender);

        verify(i18n).msg("bot.language.selected",
                "ru", "English");
    }

    private UpdateContext createCallbackContext(long userId,
            String callbackData, String langCode)
            throws Exception {
        var user = new User(userId);
        if (langCode != null) {
            setField(user, "language_code", langCode);
        }
        var callbackQuery = new CallbackQuery();
        setField(callbackQuery, "id", "cb_123");
        setField(callbackQuery, "from", user);
        setField(callbackQuery, "data", callbackData);
        var update = new Update();
        setField(update, "callback_query", callbackQuery);
        return new UpdateContext(update);
    }

    private static void setField(Object obj, String fieldName,
            Object value) throws Exception {
        Field field = findField(obj.getClass(), fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private static Field findField(Class<?> clazz, String name)
            throws NoSuchFieldException {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}

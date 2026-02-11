package com.advertmarket.communication.bot.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.shared.i18n.LocalizationService;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.SendMessage;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LanguageCommand")
class LanguageCommandTest {

    private final LocalizationService i18n =
            mock(LocalizationService.class);
    private final TelegramSender sender =
            mock(TelegramSender.class);
    private final LanguageCommand command =
            new LanguageCommand(i18n);

    @Test
    @DisplayName("Sends keyboard with two language buttons")
    void handle_sendsKeyboardWithTwoButtons() throws Exception {
        when(i18n.msg(eq("bot.language.prompt"), eq("en")))
                .thenReturn("Choose language:");
        when(i18n.msg(eq("bot.language.ru"), eq("en")))
                .thenReturn("Русский");
        when(i18n.msg(eq("bot.language.en"), eq("en")))
                .thenReturn("English");

        var ctx = createContextWithLang(1L, 42L, "en");

        command.handle(ctx, sender);

        verify(sender).execute(any(SendMessage.class), eq(42L));
    }

    @Test
    @DisplayName("Uses language code from update context")
    void handle_usesLanguageCodeFromContext() throws Exception {
        when(i18n.msg(eq("bot.language.prompt"), eq("ru")))
                .thenReturn("Выберите язык:");
        when(i18n.msg(eq("bot.language.ru"), eq("ru")))
                .thenReturn("Русский");
        when(i18n.msg(eq("bot.language.en"), eq("ru")))
                .thenReturn("English");

        var ctx = createContextWithLang(1L, 42L, "ru");

        command.handle(ctx, sender);

        verify(i18n).msg("bot.language.prompt", "ru");
    }

    @Test
    @DisplayName("Falls back to Russian when language code is null")
    void handle_fallbackToRussian() throws Exception {
        when(i18n.msg(eq("bot.language.prompt"), eq("ru")))
                .thenReturn("Выберите язык:");
        when(i18n.msg(eq("bot.language.ru"), eq("ru")))
                .thenReturn("Русский");
        when(i18n.msg(eq("bot.language.en"), eq("ru")))
                .thenReturn("English");

        var ctx = createContextWithLang(1L, 42L, null);

        command.handle(ctx, sender);

        verify(i18n).msg("bot.language.prompt", "ru");
    }

    private UpdateContext createContextWithLang(long userId,
            long chatId, String langCode) throws Exception {
        var user = new User(userId);
        if (langCode != null) {
            setField(user, "language_code", langCode);
        }
        var chat = new com.pengrad.telegrambot.model.Chat();
        setField(chat, "id", chatId);
        var message = new Message();
        setField(message, "from", user);
        setField(message, "chat", chat);
        var update = new Update();
        setField(update, "message", message);
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

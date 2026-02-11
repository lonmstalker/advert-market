package com.advertmarket.communication.bot.internal.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.shared.i18n.LocalizationService;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Reply builder")
class ReplyTest {

    private final TelegramSender sender =
            mock(TelegramSender.class);

    @Test
    @DisplayName("text() creates reply with chatId and text")
    void text_createsReplyWithChatIdAndText() throws Exception {
        var ctx = createMessageContext(1L, 42L);

        Reply.text(ctx, "*Hello*").send(sender);

        verify(sender).execute(
                any(SendMessage.class), eq(42L));
    }

    @Test
    @DisplayName("localized() uses i18n with fallback to Russian")
    void localized_usesI18nWithFallback() throws Exception {
        var i18n = mock(LocalizationService.class);
        when(i18n.msg("bot.welcome", "ru"))
                .thenReturn("*Добро пожаловать\\!*");
        var ctx = createMessageContextWithLang(1L, 42L, null);

        Reply.localized(ctx, i18n, "bot.welcome").send(sender);

        verify(i18n).msg("bot.welcome", "ru");
        verify(sender).execute(
                any(SendMessage.class), eq(42L));
    }

    @Test
    @DisplayName("callback() creates reply with callbackQueryId")
    void callback_createsReplyWithCallbackId() throws Exception {
        var ctx = createCallbackContext(1L, "cb_456");

        Reply.callback(ctx).send(sender);

        verify(sender).execute(
                any(AnswerCallbackQuery.class));
    }

    @Test
    @DisplayName("callback() throws ISE when no callback query")
    void callback_throwsWhenNoCallbackQuery() throws Exception {
        var ctx = createMessageContext(1L, 42L);

        assertThatThrownBy(() -> Reply.callback(ctx))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No callback query");
    }

    @Test
    @DisplayName("keyboard() sets inline keyboard on message")
    void keyboard_setsInlineKeyboard() throws Exception {
        var ctx = createMessageContext(1L, 42L);
        var markup = KeyboardBuilder.inline()
                .callbackButton("OK", "ok")
                .build();

        Reply.text(ctx, "Choose:")
                .keyboard(markup)
                .send(sender);

        verify(sender).execute(
                any(SendMessage.class), eq(42L));
    }

    @Test
    @DisplayName("disablePreview() adds LinkPreviewOptions")
    void disablePreview_addsLinkPreviewOptions() throws Exception {
        var ctx = createMessageContext(1L, 42L);

        Reply.text(ctx, "Link: https://example\\.com")
                .disablePreview()
                .send(sender);

        verify(sender).execute(
                any(SendMessage.class), eq(42L));
    }

    @Test
    @DisplayName("callbackText() sets callback answer text")
    void callbackText_setsCallbackAnswerText() throws Exception {
        var ctx = createCallbackContext(1L, "cb_789");

        Reply.callback(ctx)
                .callbackText("Saved!")
                .send(sender);

        verify(sender).execute(
                any(AnswerCallbackQuery.class));
    }

    @Test
    @DisplayName("send() sends message with ParseMode.MarkdownV2")
    void send_sendsWithMarkdownV2() throws Exception {
        var ctx = createMessageContext(1L, 42L);

        Reply.text(ctx, "*bold* _italic_").send(sender);

        verify(sender).execute(
                any(SendMessage.class), eq(42L));
    }

    // --- Helpers ---

    private UpdateContext createMessageContext(long userId,
            long chatId) throws Exception {
        var user = new User(userId);
        var chat = new Chat();
        setField(chat, "id", chatId);
        var message = new Message();
        setField(message, "from", user);
        setField(message, "chat", chat);
        var update = new Update();
        setField(update, "message", message);
        return new UpdateContext(update);
    }

    private UpdateContext createMessageContextWithLang(long userId,
            long chatId, String langCode) throws Exception {
        var user = new User(userId);
        if (langCode != null) {
            setField(user, "language_code", langCode);
        }
        var chat = new Chat();
        setField(chat, "id", chatId);
        var message = new Message();
        setField(message, "from", user);
        setField(message, "chat", chat);
        var update = new Update();
        setField(update, "message", message);
        return new UpdateContext(update);
    }

    private UpdateContext createCallbackContext(long userId,
            String callbackId) throws Exception {
        var user = new User(userId);
        var callbackQuery = new CallbackQuery();
        setField(callbackQuery, "id", callbackId);
        setField(callbackQuery, "from", user);
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

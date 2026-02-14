package com.advertmarket.communication.bot.internal.dispatch;

import static org.assertj.core.api.Assertions.assertThat;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.ChatMemberUpdated;
import com.pengrad.telegrambot.model.InlineQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateContext")
class UpdateContextTest {

    @Test
    @DisplayName("Extracts chat ID from message")
    void chatId_fromMessage() throws Exception {
        var update = createUpdateWithMessageChat(100L, 42L);
        var ctx = new UpdateContext(update);
        assertThat(ctx.chatId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Extracts chat ID from callback message")
    void chatId_fromCallbackMessage() throws Exception {
        var update = createUpdateWithCallbackChat(200L, 55L);
        var ctx = new UpdateContext(update);
        assertThat(ctx.chatId()).isEqualTo(55L);
    }

    @Test
    @DisplayName("Falls back to user ID when no chat")
    void chatId_fallsBackToUserId() throws Exception {
        var update = createUpdateWithInlineQuery(300L);
        var ctx = new UpdateContext(update);
        assertThat(ctx.chatId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("Extracts user ID from message")
    void userId_fromMessage() throws Exception {
        var update = createUpdateWithMessageFrom(12345L);
        var ctx = new UpdateContext(update);
        assertThat(ctx.userId()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("Extracts user ID from callback query")
    void userId_fromCallbackQuery() throws Exception {
        var update = createUpdateWithCallbackFrom(67890L);
        var ctx = new UpdateContext(update);
        assertThat(ctx.userId()).isEqualTo(67890L);
    }

    @Test
    @DisplayName("Extracts user ID from inline query")
    void userId_fromInlineQuery() throws Exception {
        var update = createUpdateWithInlineQuery(11111L);
        var ctx = new UpdateContext(update);
        assertThat(ctx.userId()).isEqualTo(11111L);
    }

    @Test
    @DisplayName("Falls back to UNKNOWN_USER_ID when no user found")
    void userId_fallbackToUnknown() throws Exception {
        var update = new Update();
        setField(update, "update_id", 99999);
        var ctx = new UpdateContext(update);
        assertThat(ctx.userId())
                .isEqualTo(UpdateContext.UNKNOWN_USER_ID);
    }

    @Test
    @DisplayName("Returns message text")
    void messageText_returnsText() throws Exception {
        var update = createUpdateWithText(1L, "/start");
        var ctx = new UpdateContext(update);
        assertThat(ctx.messageText()).isEqualTo("/start");
    }

    @Test
    @DisplayName("Returns null when no message")
    void messageText_nullWithoutMessage() {
        var ctx = new UpdateContext(new Update());
        assertThat(ctx.messageText()).isNull();
    }

    @Test
    @DisplayName("Detects text message")
    void isTextMessage_true() throws Exception {
        var update = createUpdateWithText(1L, "hello");
        var ctx = new UpdateContext(update);
        assertThat(ctx.isTextMessage()).isTrue();
    }

    @Test
    @DisplayName("Returns false for message without text")
    void isTextMessage_falseWithoutText() throws Exception {
        var update = createUpdateWithMessageFrom(1L);
        var ctx = new UpdateContext(update);
        assertThat(ctx.isTextMessage()).isFalse();
    }

    @Test
    @DisplayName("Detects callback query")
    void isCallbackQuery_true() throws Exception {
        var update = createUpdateWithCallbackFrom(1L);
        var ctx = new UpdateContext(update);
        assertThat(ctx.isCallbackQuery()).isTrue();
    }

    @Test
    @DisplayName("Returns callback data")
    void callbackData_returnsData() throws Exception {
        var update = createUpdateWithCallbackData(1L, "lang:ru");
        var ctx = new UpdateContext(update);
        assertThat(ctx.callbackData()).isEqualTo("lang:ru");
    }

    @Test
    @DisplayName("Returns user language code")
    void languageCode_returnsCode() throws Exception {
        var user = new User(1L);
        setField(user, "language_code", "ru");
        var message = new Message();
        setField(message, "from", user);
        var update = new Update();
        setField(update, "message", message);
        var ctx = new UpdateContext(update);
        assertThat(ctx.languageCode()).isEqualTo("ru");
    }

    @Test
    @DisplayName("Detects my_chat_member update")
    void isMyChatMemberUpdate_true() throws Exception {
        var update = createUpdateWithMyChatMember(1L);
        var ctx = new UpdateContext(update);
        assertThat(ctx.isMyChatMemberUpdate()).isTrue();
    }

    @Test
    @DisplayName("Returns false for non my_chat_member update")
    void isMyChatMemberUpdate_false() throws Exception {
        var update = createUpdateWithText(1L, "hello");
        var ctx = new UpdateContext(update);
        assertThat(ctx.isMyChatMemberUpdate()).isFalse();
    }

    @Test
    @DisplayName("Returns myChatMember from update")
    void myChatMember_returnsValue() throws Exception {
        var update = createUpdateWithMyChatMember(1L);
        var ctx = new UpdateContext(update);
        assertThat(ctx.myChatMember()).isNotNull();
    }

    @Test
    @DisplayName("Extracts user from myChatMember")
    void user_fromMyChatMember() throws Exception {
        var update = createUpdateWithMyChatMember(77777L);
        var ctx = new UpdateContext(update);
        assertThat(ctx.userId()).isEqualTo(77777L);
    }

    // --- Helpers ---

    private Update createUpdateWithMyChatMember(long userId)
            throws Exception {
        var user = new User(userId);
        var memberUpdated = new ChatMemberUpdated();
        setField(memberUpdated, "from", user);
        var update = new Update();
        setField(update, "my_chat_member", memberUpdated);
        return update;
    }

    private Update createUpdateWithMessageFrom(long userId)
            throws Exception {
        var user = new User(userId);
        var message = new Message();
        setField(message, "from", user);
        var update = new Update();
        setField(update, "message", message);
        return update;
    }

    private Update createUpdateWithText(long userId, String text)
            throws Exception {
        var user = new User(userId);
        var message = new Message();
        setField(message, "from", user);
        setField(message, "text", text);
        var update = new Update();
        setField(update, "message", message);
        return update;
    }

    private Update createUpdateWithMessageChat(long userId,
            long chatId) throws Exception {
        var user = new User(userId);
        var chat = new Chat();
        setField(chat, "id", chatId);
        var message = new Message();
        setField(message, "from", user);
        setField(message, "chat", chat);
        var update = new Update();
        setField(update, "message", message);
        return update;
    }

    private Update createUpdateWithCallbackFrom(long userId)
            throws Exception {
        var user = new User(userId);
        var callbackQuery = new CallbackQuery();
        setField(callbackQuery, "from", user);
        var update = new Update();
        setField(update, "callback_query", callbackQuery);
        return update;
    }

    private Update createUpdateWithCallbackData(long userId,
            String data) throws Exception {
        var user = new User(userId);
        var callbackQuery = new CallbackQuery();
        setField(callbackQuery, "from", user);
        setField(callbackQuery, "data", data);
        var update = new Update();
        setField(update, "callback_query", callbackQuery);
        return update;
    }

    private Update createUpdateWithCallbackChat(long userId,
            long chatId) throws Exception {
        var user = new User(userId);
        var chat = new Chat();
        setField(chat, "id", chatId);
        var message = new Message();
        setField(message, "chat", chat);
        var callbackQuery = new CallbackQuery();
        setField(callbackQuery, "from", user);
        setField(callbackQuery, "message", message);
        var update = new Update();
        setField(update, "callback_query", callbackQuery);
        return update;
    }

    private Update createUpdateWithInlineQuery(long userId)
            throws Exception {
        var user = new User(userId);
        var inlineQuery = new InlineQuery();
        setField(inlineQuery, "from", user);
        var update = new Update();
        setField(update, "inline_query", inlineQuery);
        return update;
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

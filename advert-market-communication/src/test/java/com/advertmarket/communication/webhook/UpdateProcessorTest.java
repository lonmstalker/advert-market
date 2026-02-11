package com.advertmarket.communication.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.InlineQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class UpdateProcessorTest {

    @Test
    void extractUserId_fromMessage() throws Exception {
        var update = createUpdateWithMessageFrom(12345L);
        assertThat(UpdateProcessor.extractUserId(update)).isEqualTo(12345L);
    }

    @Test
    void extractUserId_fromCallbackQuery() throws Exception {
        var update = createUpdateWithCallbackFrom(67890L);
        assertThat(UpdateProcessor.extractUserId(update)).isEqualTo(67890L);
    }

    @Test
    void extractUserId_fromInlineQuery() throws Exception {
        var update = createUpdateWithInlineQueryFrom(11111L);
        assertThat(UpdateProcessor.extractUserId(update)).isEqualTo(11111L);
    }

    @Test
    void extractUserId_fallbackToUpdateId() throws Exception {
        var update = new Update();
        setField(update, "update_id", 99999);
        assertThat(UpdateProcessor.extractUserId(update)).isEqualTo(99999);
    }

    // Helper methods using reflection to create Update objects
    // (pengrad library does not expose constructors for test purposes)

    private Update createUpdateWithMessageFrom(long userId) throws Exception {
        var user = new User(userId);
        var message = new Message();
        setField(message, "from", user);
        var update = new Update();
        setField(update, "message", message);
        return update;
    }

    private Update createUpdateWithCallbackFrom(long userId) throws Exception {
        var user = new User(userId);
        var callbackQuery = new CallbackQuery();
        setField(callbackQuery, "from", user);
        var update = new Update();
        setField(update, "callback_query", callbackQuery);
        return update;
    }

    private Update createUpdateWithInlineQueryFrom(long userId) throws Exception {
        var user = new User(userId);
        var inlineQuery = new InlineQuery();
        setField(inlineQuery, "from", user);
        var update = new Update();
        setField(update, "inline_query", inlineQuery);
        return update;
    }

    private static void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = findField(obj.getClass(), fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
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

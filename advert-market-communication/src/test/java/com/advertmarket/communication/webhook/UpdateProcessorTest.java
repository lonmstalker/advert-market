package com.advertmarket.communication.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.bot.internal.dispatch.BotDispatcher;
import com.advertmarket.communication.bot.internal.error.BotErrorHandler;
import com.advertmarket.communication.canary.CanaryRouter;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.InlineQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateProcessor")
class UpdateProcessorTest {

    @Test
    @DisplayName("Extracts user ID from message")
    void extractUserId_fromMessage() throws Exception {
        var update = createUpdateWithMessageFrom(12345L);
        assertThat(UpdateProcessor.extractUserId(update)).isEqualTo(12345L);
    }

    @Test
    @DisplayName("Extracts user ID from callback query")
    void extractUserId_fromCallbackQuery() throws Exception {
        var update = createUpdateWithCallbackFrom(67890L);
        assertThat(UpdateProcessor.extractUserId(update)).isEqualTo(67890L);
    }

    @Test
    @DisplayName("Extracts user ID from inline query")
    void extractUserId_fromInlineQuery() throws Exception {
        var update = createUpdateWithInlineQueryFrom(11111L);
        assertThat(UpdateProcessor.extractUserId(update)).isEqualTo(11111L);
    }

    @Test
    @DisplayName("Falls back to update ID when no user found")
    void extractUserId_fallbackToUpdateId() throws Exception {
        var update = new Update();
        setField(update, "update_id", 99999);
        assertThat(UpdateProcessor.extractUserId(update)).isEqualTo(99999);
    }

    @Test
    @DisplayName("Does not throw when executor rejects dispatch submit")
    void processAsync_executorRejected_doesNotThrow() throws Exception {
        var canaryRouter = mock(CanaryRouter.class);
        var dispatcher = mock(BotDispatcher.class);
        var errorHandler = mock(BotErrorHandler.class);
        var metrics = mock(MetricsFacade.class);
        var executor = mock(ExecutorService.class);

        when(canaryRouter.isCanary(anyLong())).thenReturn(false);
        when(executor.submit(any(Runnable.class)))
                .thenThrow(new RejectedExecutionException("queue full"));

        var processor = new UpdateProcessor(
                canaryRouter, dispatcher, errorHandler, metrics, executor);
        var update = createUpdateWithMessageFrom(12345L);
        setField(update, "update_id", 77);

        assertThatCode(() -> processor.processAsync(update))
                .doesNotThrowAnyException();
        verify(metrics).incrementCounter(
                MetricNames.WEBHOOK_DISPATCH_REJECTED,
                "reason", "rejected_execution");
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

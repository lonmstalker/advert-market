package com.advertmarket.communication.bot.internal.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.bot.internal.block.UserBlockPort;
import com.advertmarket.communication.bot.internal.error.BotErrorHandler;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.shared.i18n.LocalizationService;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.model.ChatMemberUpdated;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BotDispatcher")
class BotDispatcherTest {

    private final TelegramSender sender =
            mock(TelegramSender.class);
    private final BotErrorHandler errorHandler =
            mock(BotErrorHandler.class);
    private final UserBlockPort blockPort =
            mock(UserBlockPort.class);
    private final LocalizationService i18n =
            mock(LocalizationService.class);

    @Test
    @DisplayName("Dispatches command to matching handler")
    void dispatchesCommand() throws Exception {
        var handled = new AtomicBoolean();
        BotCommand cmd = new BotCommand() {
            @Override
            public String command() {
                return "/test";
            }

            @Override
            public void handle(UpdateContext ctx,
                    TelegramSender s) {
                handled.set(true);
            }
        };
        var dispatcher = dispatcher(
                List.of(cmd), List.of(), List.of(), List.of());
        var ctx = new UpdateContext(
                createUpdateWithText(1L, "/test arg"));

        dispatcher.dispatch(ctx);

        assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("Dispatches callback to matching handler")
    void dispatchesCallback() throws Exception {
        var handled = new AtomicBoolean();
        CallbackHandler handler = new CallbackHandler() {
            @Override
            public String prefix() {
                return "action:";
            }

            @Override
            public void handle(UpdateContext ctx,
                    TelegramSender s) {
                handled.set(true);
            }
        };
        var dispatcher = dispatcher(
                List.of(), List.of(handler), List.of(), List.of());
        var ctx = new UpdateContext(
                createUpdateWithCallback(1L, "action:123"));

        dispatcher.dispatch(ctx);

        assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("Longest prefix wins for callback routing")
    void callbackLongestPrefixWins() throws Exception {
        var shortHandled = new AtomicBoolean();
        var longHandled = new AtomicBoolean();
        CallbackHandler shortHandler = new CallbackHandler() {
            @Override
            public String prefix() {
                return "a:";
            }

            @Override
            public void handle(UpdateContext ctx,
                    TelegramSender s) {
                shortHandled.set(true);
            }
        };
        CallbackHandler longHandler = new CallbackHandler() {
            @Override
            public String prefix() {
                return "a:detail:";
            }

            @Override
            public void handle(UpdateContext ctx,
                    TelegramSender s) {
                longHandled.set(true);
            }
        };
        var dispatcher = dispatcher(List.of(),
                List.of(shortHandler, longHandler),
                List.of(), List.of());
        var ctx = new UpdateContext(
                createUpdateWithCallback(1L, "a:detail:xyz"));

        dispatcher.dispatch(ctx);

        assertThat(longHandled).isTrue();
        assertThat(shortHandled).isFalse();
    }

    @Test
    @DisplayName("Delegates command errors to error handler")
    void commandErrorDelegatesToHandler() throws Exception {
        BotCommand cmd = new BotCommand() {
            @Override
            public String command() {
                return "/fail";
            }

            @Override
            public void handle(UpdateContext ctx,
                    TelegramSender s) {
                throw new RuntimeException("boom");
            }
        };
        var dispatcher = dispatcher(
                List.of(cmd), List.of(), List.of(), List.of());
        var update = createUpdateWithText(1L, "/fail");
        setField(update, "update_id", 42);
        var ctx = new UpdateContext(update);

        dispatcher.dispatch(ctx);

        verify(errorHandler).handleAndNotify(
                org.mockito.ArgumentMatchers
                        .any(Exception.class),
                org.mockito.ArgumentMatchers.eq(42),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.nullable(
                        String.class));
    }

    @Test
    @DisplayName("Ignores unknown commands")
    void unknownCommandIgnored() throws Exception {
        var dispatcher = dispatcher(
                List.of(), List.of(), List.of(), List.of());
        var ctx = new UpdateContext(
                createUpdateWithText(1L, "/unknown"));

        dispatcher.dispatch(ctx);

        verifyNoInteractions(errorHandler);
    }

    @Test
    @DisplayName("Strips bot username from command")
    void stripsBotnameFromCommand() throws Exception {
        var handled = new AtomicBoolean();
        BotCommand cmd = new BotCommand() {
            @Override
            public String command() {
                return "/start";
            }

            @Override
            public void handle(UpdateContext ctx,
                    TelegramSender s) {
                handled.set(true);
            }
        };
        var dispatcher = dispatcher(
                List.of(cmd), List.of(), List.of(), List.of());
        var ctx = new UpdateContext(
                createUpdateWithText(1L, "/start@MyBot"));

        dispatcher.dispatch(ctx);

        assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("Blocked user receives block message")
    void blockedUserReceivesBlockMessage() throws Exception {
        when(blockPort.isBlocked(1L)).thenReturn(true);
        when(i18n.msg("bot.blocked", "ru"))
                .thenReturn("Blocked");
        var dispatcher = dispatcher(
                List.of(), List.of(), List.of(), List.of());
        var ctx = new UpdateContext(
                createUpdateWithText(1L, "/start"));

        dispatcher.dispatch(ctx);

        verify(blockPort).isBlocked(1L);
    }

    @Test
    @DisplayName("Dispatches to message handler")
    void dispatchesMessageHandler() throws Exception {
        var handled = new AtomicBoolean();
        MessageHandler handler = new MessageHandler() {
            @Override
            public boolean canHandle(UpdateContext ctx) {
                return true;
            }

            @Override
            public void handle(UpdateContext ctx,
                    TelegramSender s) {
                handled.set(true);
            }
        };
        var dispatcher = dispatcher(
                List.of(), List.of(), List.of(handler),
                List.of());
        var ctx = new UpdateContext(
                createUpdateWithText(1L, "hello world"));

        dispatcher.dispatch(ctx);

        assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("Dispatches my_chat_member to matching handler")
    void dispatchesChatMemberUpdate() throws Exception {
        var handled = new AtomicBoolean();
        var cmHandler = chatMemberHandler(handled, true);
        var dispatcher = dispatcher(
                List.of(), List.of(), List.of(),
                List.of(cmHandler));
        var ctx = new UpdateContext(
                createUpdateWithMyChatMember(1L, -100L));

        dispatcher.dispatch(ctx);

        assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("my_chat_member skips block check")
    void chatMemberUpdate_skipsBlockCheck() throws Exception {
        when(blockPort.isBlocked(1L)).thenReturn(true);
        var handled = new AtomicBoolean();
        var cmHandler = chatMemberHandler(handled, true);
        var dispatcher = dispatcher(
                List.of(), List.of(), List.of(),
                List.of(cmHandler));
        var ctx = new UpdateContext(
                createUpdateWithMyChatMember(1L, -100L));

        dispatcher.dispatch(ctx);

        assertThat(handled).isTrue();
        verifyNoInteractions(blockPort);
    }

    @Test
    @DisplayName("Unhandled my_chat_member is silently ignored")
    void chatMemberUpdate_unhandledIgnored() throws Exception {
        var cmHandler = chatMemberHandler(
                new AtomicBoolean(), false);
        var dispatcher = dispatcher(
                List.of(), List.of(), List.of(),
                List.of(cmHandler));
        var ctx = new UpdateContext(
                createUpdateWithMyChatMember(1L, -100L));

        dispatcher.dispatch(ctx);

        verifyNoInteractions(errorHandler);
    }

    // --- Factory helpers ---

    private BotDispatcher dispatcher(
            List<BotCommand> cmds,
            List<CallbackHandler> cbs,
            List<MessageHandler> msgs,
            List<ChatMemberUpdateHandler> cms) {
        var registry = new HandlerRegistry(
                cmds, cbs, msgs, cms);
        return new BotDispatcher(registry, sender,
                errorHandler, blockPort, i18n);
    }

    private ChatMemberUpdateHandler chatMemberHandler(
            AtomicBoolean handled, boolean accepts) {
        return new ChatMemberUpdateHandler() {
            @Override
            public boolean canHandle(
                    ChatMemberUpdated update) {
                return accepts;
            }

            @Override
            public void handle(
                    ChatMemberUpdated update) {
                handled.set(true);
            }
        };
    }

    // --- Telegram model helpers ---

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

    private Update createUpdateWithMyChatMember(long userId,
            long chatId) throws Exception {
        var chat = new Chat();
        setField(chat, "id", chatId);
        setField(chat, "type", Chat.Type.channel);
        var oldMember = new ChatMember();
        setField(oldMember, "status",
                ChatMember.Status.administrator);
        var newMember = new ChatMember();
        setField(newMember, "status", ChatMember.Status.left);
        var memberUpdated = new ChatMemberUpdated();
        setField(memberUpdated, "chat", chat);
        setField(memberUpdated, "from", new User(userId));
        setField(memberUpdated, "old_chat_member", oldMember);
        setField(memberUpdated, "new_chat_member", newMember);
        var update = new Update();
        setField(update, "my_chat_member", memberUpdated);
        return update;
    }

    private Update createUpdateWithCallback(long userId,
            String data) throws Exception {
        var user = new User(userId);
        var callbackQuery = new CallbackQuery();
        setField(callbackQuery, "from", user);
        setField(callbackQuery, "data", data);
        var update = new Update();
        setField(update, "callback_query", callbackQuery);
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
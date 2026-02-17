package com.advertmarket.communication.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.bot.internal.config.TelegramBotProperties;
import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.i18n.LocalizationService;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("StartCommand")
class StartCommandTest {

    @Test
    @DisplayName("Prefixes welcome message with custom emoji when configured")
    void maybePrefixCustomEmoji_withId() {
        assertThat(StartCommand.maybePrefixCustomEmoji(
                "Hello", "123"))
                .isEqualTo("![⭐](tg://emoji?id=123) Hello");
    }

    @Test
    @DisplayName("Does not prefix welcome message when emoji id is blank")
    void maybePrefixCustomEmoji_blank() {
        assertThat(StartCommand.maybePrefixCustomEmoji(
                "Hello", ""))
                .isEqualTo("Hello");
    }

    @Test
    @DisplayName("Does not prefix welcome message when emoji id is invalid")
    void maybePrefixCustomEmoji_invalid() {
        assertThat(StartCommand.maybePrefixCustomEmoji(
                "Hello", "abc"))
                .isEqualTo("Hello");
    }

    @Test
    @DisplayName("Extracts deep link parameter")
    void extractDeepLinkParam_withParam() {
        assertThat(StartCommand.extractDeepLinkParam(
                "/start deal_123")).isEqualTo("deal_123");
    }

    @Test
    @DisplayName("Returns null when no parameter")
    void extractDeepLinkParam_noParam() {
        assertThat(StartCommand.extractDeepLinkParam("/start"))
                .isNull();
    }

    @Test
    @DisplayName("Returns null for null text")
    void extractDeepLinkParam_nullText() {
        assertThat(StartCommand.extractDeepLinkParam(null))
                .isNull();
    }

    @Test
    @DisplayName("Resolves deal deep link route")
    void resolveRoute_deal() {
        assertThat(StartCommand.resolveRoute("deal_abc"))
                .isEqualTo("/deals/abc");
    }

    @Test
    @DisplayName("Resolves channel deep link route")
    void resolveRoute_channel() {
        assertThat(StartCommand.resolveRoute("channel_xyz"))
                .isEqualTo("/catalog/channels/xyz");
    }

    @Test
    @DisplayName("Resolves dispute deep link route")
    void resolveRoute_dispute() {
        assertThat(StartCommand.resolveRoute("dispute_99"))
                .isEqualTo("/deals/99");
    }

    @Test
    @DisplayName("Resolves deposit deep link route")
    void resolveRoute_deposit() {
        assertThat(StartCommand.resolveRoute("deposit_42"))
                .isEqualTo("/deals/42");
    }

    @Test
    @DisplayName("Falls back to ref parameter for unknown prefix")
    void resolveRoute_unknownPrefix() {
        assertThat(StartCommand.resolveRoute("unknown_param"))
                .isEqualTo("/?ref=unknown_param");
    }

    @Nested
    @DisplayName("handle — user upsert")
    class HandleUpsert {

        private UserRepository userRepository;
        private LocalizationService i18n;
        private TelegramSender sender;
        private StartCommand command;

        @BeforeEach
        void setUp() {
            userRepository = mock(UserRepository.class);
            i18n = mock(LocalizationService.class);
            sender = mock(TelegramSender.class);

            when(i18n.msg(anyString(), anyString()))
                    .thenReturn("Welcome");

            var botProps = new TelegramBotProperties(
                    "token", "bot",
                    new TelegramBotProperties.Webhook(
                            "", "secret", 262_144),
                    new TelegramBotProperties.WebApp("https://app.test"),
                    new TelegramBotProperties.Welcome(""));

            command = new StartCommand(botProps, i18n, userRepository);
        }

        @Test
        @DisplayName("Should upsert user with Telegram data on /start")
        void upsertsUserOnStart() throws Exception {
            var ctx = createMessageContext(
                    42L, "John", "Doe", "johndoe", "en",
                    "/start");

            command.handle(ctx, sender);

            var captor = ArgumentCaptor.forClass(
                    TelegramUserData.class);
            verify(userRepository).upsert(captor.capture());

            var data = captor.getValue();
            assertThat(data.id()).isEqualTo(42L);
            assertThat(data.firstName()).isEqualTo("John");
            assertThat(data.lastName()).isEqualTo("Doe");
            assertThat(data.username()).isEqualTo("johndoe");
            assertThat(data.languageCode()).isEqualTo("en");
        }

        @Test
        @DisplayName("Should upsert user with null optional fields")
        void upsertsUserWithNullOptionals() throws Exception {
            var ctx = createMessageContext(
                    99L, "Alice", null, null, null,
                    "/start");

            command.handle(ctx, sender);

            var captor = ArgumentCaptor.forClass(
                    TelegramUserData.class);
            verify(userRepository).upsert(captor.capture());

            var data = captor.getValue();
            assertThat(data.id()).isEqualTo(99L);
            assertThat(data.firstName()).isEqualTo("Alice");
            assertThat(data.lastName()).isNull();
            assertThat(data.username()).isNull();
        }

        @Test
        @DisplayName("Should upsert user on deep link /start")
        void upsertsUserOnDeepLink() throws Exception {
            var ctx = createMessageContext(
                    55L, "Bob", null, "bob", "ru",
                    "/start deal_abc");

            command.handle(ctx, sender);

            verify(userRepository).upsert(any(TelegramUserData.class));
        }

        @Test
        @DisplayName("Should not fail when user is null in update")
        void handlesNullUser() throws Exception {
            final var update = new Update();
            var message = new Message();
            setField(message, "text", "/start");
            var chat = new Chat();
            setField(chat, "id", 1L);
            setField(message, "chat", chat);
            setField(update, "message", message);
            // No 'from' field set — user() returns null
            var ctx = new UpdateContext(update);

            command.handle(ctx, sender);

            verify(userRepository, never())
                    .upsert(any(TelegramUserData.class));
        }
    }

    private static UpdateContext createMessageContext(
            long userId, String firstName, String lastName,
            String username, String langCode, String text)
            throws Exception {
        var user = new User(userId);
        setField(user, "first_name", firstName);
        if (lastName != null) {
            setField(user, "last_name", lastName);
        }
        if (username != null) {
            setField(user, "username", username);
        }
        if (langCode != null) {
            setField(user, "language_code", langCode);
        }

        var chat = new Chat();
        setField(chat, "id", userId);

        var message = new Message();
        setField(message, "from", user);
        setField(message, "text", text);
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

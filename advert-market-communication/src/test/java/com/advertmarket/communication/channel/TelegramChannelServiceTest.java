package com.advertmarket.communication.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.api.channel.ChatInfo;
import com.advertmarket.communication.api.channel.ChatMemberInfo;
import com.advertmarket.communication.api.channel.ChatMemberStatus;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.communication.channel.internal.ChannelCachePort;
import com.advertmarket.communication.channel.internal.ChannelRateLimiterPort;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.model.ChatFullInfo;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.GetChat;
import com.pengrad.telegrambot.request.GetChatAdministrators;
import com.pengrad.telegrambot.request.GetChatMemberCount;
import com.pengrad.telegrambot.response.GetChatAdministratorsResponse;
import com.pengrad.telegrambot.response.GetChatMemberCountResponse;
import com.pengrad.telegrambot.response.GetChatResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TelegramChannelService")
class TelegramChannelServiceTest {

    private TelegramSender sender;
    private ChannelCachePort cache;
    private ChannelRateLimiterPort rateLimiter;
    private MetricsFacade metrics;
    private TelegramChannelService service;

    @BeforeEach
    void setUp() {
        sender = mock(TelegramSender.class);
        cache = mock(ChannelCachePort.class);
        rateLimiter = mock(ChannelRateLimiterPort.class);
        when(rateLimiter.acquire(any(Long.class))).thenReturn(true);
        metrics = new MetricsFacade(new SimpleMeterRegistry());
        service = new TelegramChannelService(
                sender, cache, rateLimiter, metrics);
    }

    @Nested
    @DisplayName("getChat")
    class GetChatTests {

        @Test
        @DisplayName("Returns cached data on cache hit")
        void returnsFromCache() {
            var cached = new ChatInfo(
                    123L, "MyChannel", "mychan",
                    "channel", "desc");
            when(cache.getChatInfo(123L))
                    .thenReturn(Optional.of(cached));

            ChatInfo result = service.getChat(123L);

            assertThat(result).isEqualTo(cached);
            verifyNoInteractions(sender);
        }

        @Test
        @DisplayName("Calls API on cache miss and caches result")
        void callsApiOnCacheMiss() {
            when(cache.getChatInfo(123L))
                    .thenReturn(Optional.empty());
            var response = buildGetChatResponse(
                    123L, "Test", "test",
                    ChatFullInfo.Type.channel, "desc");
            when(sender.execute(any(GetChat.class)))
                    .thenReturn(response);

            ChatInfo result = service.getChat(123L);

            assertThat(result.id()).isEqualTo(123L);
            assertThat(result.title()).isEqualTo("Test");
            verify(cache).putChatInfo(
                    any(Long.class), any(ChatInfo.class));
        }

        @Test
        @DisplayName("Throws CHANNEL_NOT_FOUND on 400 error")
        void throwsChannelNotFoundOn400() {
            when(cache.getChatInfo(123L))
                    .thenReturn(Optional.empty());
            var response = buildErrorResponse(
                    GetChatResponse.class, 400,
                    "Bad Request: chat not found");
            when(sender.execute(any(GetChat.class)))
                    .thenReturn(response);

            assertThatThrownBy(() -> service.getChat(123L))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo(
                                    ErrorCodes.CHANNEL_NOT_FOUND));
        }

        @Test
        @DisplayName("Throws CHAN_BOT_NOT_MEMBER on 403 error")
        void throwsBotNotMemberOn403() {
            when(cache.getChatInfo(123L))
                    .thenReturn(Optional.empty());
            var response = buildErrorResponse(
                    GetChatResponse.class, 403,
                    "Forbidden: bot is not a member");
            when(sender.execute(any(GetChat.class)))
                    .thenReturn(response);

            assertThatThrownBy(() -> service.getChat(123L))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo(
                                    ErrorCodes.CHAN_BOT_NOT_MEMBER));
        }

        @Test
        @DisplayName("Falls back to stale cache on circuit breaker open")
        void fallsBackToStaleCacheOnCbOpen() {
            when(cache.getChatInfo(123L))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(new ChatInfo(
                            123L, "Stale", null,
                            "channel", null)));
            when(sender.execute(any(GetChat.class)))
                    .thenThrow(mock(
                            CallNotPermittedException.class));

            ChatInfo result = service.getChat(123L);

            assertThat(result.title()).isEqualTo("Stale");
        }

        @Test
        @DisplayName("Throws SERVICE_UNAVAILABLE when CB open and no cache")
        void throwsServiceUnavailableWhenNoCacheAndCbOpen() {
            when(cache.getChatInfo(123L))
                    .thenReturn(Optional.empty());
            when(sender.execute(any(GetChat.class)))
                    .thenThrow(mock(
                            CallNotPermittedException.class));

            assertThatThrownBy(() -> service.getChat(123L))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo(
                                    ErrorCodes.SERVICE_UNAVAILABLE));
        }
    }

    @Nested
    @DisplayName("getChatAdministrators")
    class GetChatAdministratorsTests {

        @Test
        @DisplayName("Returns cached admins on cache hit")
        void returnsFromCache() {
            var cachedAdmins = List.of(new ChatMemberInfo(
                    42L, ChatMemberStatus.CREATOR,
                    true, true, true, true));
            when(cache.getAdministrators(123L))
                    .thenReturn(Optional.of(cachedAdmins));

            var result = service.getChatAdministrators(123L);

            assertThat(result).hasSize(1);
            verifyNoInteractions(sender);
        }

        @Test
        @DisplayName("Calls API on cache miss and caches result")
        void callsApiOnCacheMiss() {
            when(cache.getAdministrators(123L))
                    .thenReturn(Optional.empty());
            var response = buildGetAdminsResponse(
                    List.of(buildChatMember(
                            ChatMember.Status.creator, 42L)));
            when(sender.execute(
                    any(GetChatAdministrators.class)))
                    .thenReturn(response);

            var result = service.getChatAdministrators(123L);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().status())
                    .isEqualTo(ChatMemberStatus.CREATOR);
            verify(cache).putAdministrators(
                    any(Long.class), any());
        }
    }

    @Nested
    @DisplayName("getChatMemberCount")
    class GetChatMemberCountTests {

        @Test
        @DisplayName("Returns member count from API")
        void returnsMemberCount() {
            var response = buildGetMemberCountResponse(1500);
            when(sender.execute(any(GetChatMemberCount.class)))
                    .thenReturn(response);

            int count = service.getChatMemberCount(123L);

            assertThat(count).isEqualTo(1500);
        }

        @Test
        @DisplayName("Throws SERVICE_UNAVAILABLE on circuit breaker open")
        void throwsOnCbOpen() {
            when(sender.execute(any(GetChatMemberCount.class)))
                    .thenThrow(mock(
                            CallNotPermittedException.class));

            assertThatThrownBy(
                    () -> service.getChatMemberCount(123L))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo(
                                    ErrorCodes.SERVICE_UNAVAILABLE));
        }
    }

    // --- Test helpers ---

    @SuppressWarnings("unchecked")
    private static <T> T buildErrorResponse(
            Class<T> type, int errorCode, String description) {
        try {
            T response = type.getDeclaredConstructor()
                    .newInstance();
            setField(response, "ok", false);
            setField(response, "error_code", errorCode);
            setField(response, "description", description);
            return response;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static GetChatResponse buildGetChatResponse(
            long id, String title, String username,
            ChatFullInfo.Type type, String description) {
        var chat = new ChatFullInfo();
        setField(chat, "id", id);
        setField(chat, "title", title);
        setField(chat, "username", username);
        setField(chat, "type", type);
        setField(chat, "description", description);

        var response = new GetChatResponse();
        setField(response, "ok", true);
        setField(response, "result", chat);
        return response;
    }

    private static GetChatAdministratorsResponse
            buildGetAdminsResponse(List<ChatMember> admins) {
        var response = new GetChatAdministratorsResponse();
        setField(response, "ok", true);
        setField(response, "result", admins);
        return response;
    }

    private static GetChatMemberCountResponse
            buildGetMemberCountResponse(int count) {
        var response = new GetChatMemberCountResponse();
        setField(response, "ok", true);
        setField(response, "result", count);
        return response;
    }

    private static ChatMember buildChatMember(
            ChatMember.Status status, long userId) {
        ChatMember cm = new ChatMember();
        setField(cm, "status", status);
        User user = new User(userId);
        setField(cm, "user", user);
        return cm;
    }

    private static void setField(Object target,
            String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to set field " + fieldName
                            + " on " + target.getClass()
                                    .getSimpleName(), e);
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException(
                "Field not found: " + name
                        + " in " + clazz.getSimpleName());
    }
}

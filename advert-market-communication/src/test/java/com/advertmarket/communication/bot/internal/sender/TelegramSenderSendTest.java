package com.advertmarket.communication.bot.internal.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.bot.internal.builder.KeyboardBuilder;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("TelegramSender send methods")
class TelegramSenderSendTest {

    private TelegramBot bot;
    private TelegramSender sender;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        doAnswer(invocation -> {
            Callback cb = invocation.getArgument(1);
            var response = mock(BaseResponse.class);
            when(response.isOk()).thenReturn(true);
            cb.onResponse(invocation.getArgument(0), response);
            return null;
        }).when(bot).execute(
                any(BaseRequest.class), any(Callback.class));

        var rateLimiter = mock(RateLimiterPort.class);
        var cb = CircuitBreaker.ofDefaults("test");
        var bulkhead = Bulkhead.ofDefaults("test");
        var metrics = new MetricsFacade(
                new SimpleMeterRegistry());
        var retryProps = new TelegramRetryProperties(
                1, List.of(Duration.ofSeconds(1)));
        var executor = Executors
                .newVirtualThreadPerTaskExecutor();
        sender = new TelegramSender(bot, rateLimiter,
                cb, bulkhead, metrics, retryProps, executor);
    }

    @Test
    @DisplayName("send(chatId, text) creates SendMessage with MarkdownV2")
    void send_createsMarkdownV2Message() {
        sender.send(42L, "*Hello*");

        var captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(captor.capture(),
                any(Callback.class));
    }

    @Test
    @DisplayName("send(chatId, text, keyboard) adds reply markup")
    void send_withKeyboard_addsReplyMarkup() {
        var keyboard = KeyboardBuilder.inline()
                .callbackButton("OK", "ok")
                .build();

        sender.send(42L, "*Choose:*", keyboard);

        var captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(captor.capture(),
                any(Callback.class));
    }

    @Test
    @DisplayName("send(chatId, text) falls back to plain text when Telegram rejects MarkdownV2")
    void send_fallsBackToPlainText_onMarkdownV2ParseError() {
        reset(bot);
        var calls = new AtomicInteger(0);
        doAnswer(invocation -> {
            Callback cb = invocation.getArgument(1);
            int n = calls.incrementAndGet();

            var response = mock(SendResponse.class);
            if (n == 1) {
                when(response.isOk()).thenReturn(false);
                when(response.errorCode()).thenReturn(400);
                when(response.description()).thenReturn(
                        "Bad Request: can't parse entities: "
                                + "Character '.' is reserved and must be "
                                + "escaped with the preceding '\\\\'");
            } else {
                when(response.isOk()).thenReturn(true);
            }

            cb.onResponse(invocation.getArgument(0), response);
            return null;
        }).when(bot).execute(
                any(BaseRequest.class), any(Callback.class));

        sender.send(42L, "*Hello*.");

        var captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot, times(2)).execute(captor.capture(), any(Callback.class));

        var sent = captor.getAllValues();
        assertThat(sent).hasSize(2);
        // 1st: our normal MarkdownV2 send
        assertThat(sent.get(0).getParseMode())
                .isEqualTo(ParseMode.MarkdownV2);
        // 2nd: fallback without parse mode to avoid parse errors
        assertThat(sent.get(1).getParseMode()).isNull();
        assertThat(sent.get(1).getText()).isEqualTo("*Hello*.");
    }

    @Test
    @DisplayName("answerCallback(id) creates AnswerCallbackQuery")
    void answerCallback_createsQuery() {
        sender.answerCallback("cb_123");

        verify(bot).execute(
                any(AnswerCallbackQuery.class),
                any(Callback.class));
    }

    @Test
    @DisplayName("answerCallback(id, text) adds text to answer")
    void answerCallback_withText_addsText() {
        sender.answerCallback("cb_456", "Done!");

        verify(bot).execute(
                any(AnswerCallbackQuery.class),
                any(Callback.class));
    }
}

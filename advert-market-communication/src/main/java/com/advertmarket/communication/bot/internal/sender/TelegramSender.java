package com.advertmarket.communication.bot.internal.sender;

import static com.advertmarket.communication.bot.internal.BotConstants.METRIC_API_CALL;

import com.advertmarket.communication.bot.internal.error.BotErrorCategory;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Sends messages via the Telegram Bot API with rate limiting,
 * circuit breaking, and async retry logic.
 */
@Slf4j
@Component
public class TelegramSender {

    private final TelegramBot bot;
    private final RateLimiterPort rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final MetricsFacade metrics;
    private final TelegramRetryProperties retryProperties;
    private final Executor executor;

    /** Creates the sender with all required dependencies. */
    public TelegramSender(TelegramBot bot,
            RateLimiterPort rateLimiter,
            CircuitBreaker circuitBreaker,
            Bulkhead bulkhead,
            MetricsFacade metrics,
            TelegramRetryProperties retryProperties,
            @Qualifier("botUpdateExecutor")
            Executor executor) {
        this.bot = bot;
        this.rateLimiter = rateLimiter;
        this.circuitBreaker = circuitBreaker;
        this.bulkhead = bulkhead;
        this.metrics = metrics;
        this.retryProperties = retryProperties;
        this.executor = executor;
    }

    /** Sends an HTML text message to the given chat. */
    public void send(long chatId, @NonNull String htmlText) {
        var request = new SendMessage(chatId, htmlText)
                .parseMode(ParseMode.HTML);
        execute(request, chatId);
    }

    /** Sends an HTML message with an inline keyboard. */
    public void send(long chatId, @NonNull String htmlText,
            @NonNull InlineKeyboardMarkup keyboard) {
        var request = new SendMessage(chatId, htmlText)
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboard);
        execute(request, chatId);
    }

    /** Answers a callback query without text. */
    public void answerCallback(@NonNull String callbackQueryId) {
        execute(new AnswerCallbackQuery(callbackQueryId));
    }

    /** Answers a callback query with a text notification. */
    public void answerCallback(@NonNull String callbackQueryId,
            @NonNull String text) {
        execute(new AnswerCallbackQuery(callbackQueryId)
                .text(text));
    }

    /**
     * Executes a request with chat-level rate limiting.
     *
     * @param request the request to execute
     * @param chatId  the target chat id for rate limiting
     * @param <T>     the request type
     * @param <R>     the response type
     * @return the Telegram API response
     */
    public <T extends BaseRequest<T, R>, R extends BaseResponse>
            R execute(T request, long chatId) {
        return executeAsync(request, chatId).join();
    }

    /**
     * Executes a request without chat-level rate limiting.
     *
     * @param request the request to execute
     * @param <T>     the request type
     * @param <R>     the response type
     * @return the Telegram API response
     */
    public <T extends BaseRequest<T, R>, R extends BaseResponse>
            R execute(T request) {
        return executeWithResilienceAsync(request)
                .toCompletableFuture().join();
    }

    /**
     * Executes a request asynchronously with rate limiting.
     *
     * @param request the request to execute
     * @param chatId  the target chat id for rate limiting
     * @param <T>     the request type
     * @param <R>     the response type
     * @return a future with the Telegram API response
     */
    public <T extends BaseRequest<T, R>, R extends BaseResponse>
            CompletableFuture<R> executeAsync(
                    T request, long chatId) {
        return CompletableFuture
                .runAsync(
                        () -> rateLimiter.acquire(chatId),
                        executor)
                .thenCompose(_ ->
                        executeWithResilienceAsync(request)
                                .toCompletableFuture());
    }

    private <T extends BaseRequest<T, R>, R extends BaseResponse>
            CompletionStage<R> executeWithResilienceAsync(
                    T request) {
        return circuitBreaker.executeCompletionStage(
                () -> bulkhead.executeCompletionStage(
                        () -> executeWithRetryAsync(request, 0)));
    }

    private <T extends BaseRequest<T, R>, R extends BaseResponse>
            CompletableFuture<R> executeWithRetryAsync(
                    T request, int attempt) {
        var future = new CompletableFuture<R>();
        bot.execute(request, new Callback<T, R>() {
            @Override
            public void onResponse(T req, R response) {
                metrics.incrementCounter(METRIC_API_CALL,
                        "ok", String.valueOf(response.isOk()));
                if (response.isOk()) {
                    future.complete(response);
                    return;
                }
                handleFailedResponse(
                        request, response, attempt, future);
            }

            @Override
            public void onFailure(T req, IOException e) {
                metrics.incrementCounter(METRIC_API_CALL,
                        "ok", "false");
                handleException(
                        request, e, attempt, future);
            }
        });
        return future;
    }

    private <T extends BaseRequest<T, R>, R extends BaseResponse>
            void handleFailedResponse(T request,
                    R response, int attempt,
                    CompletableFuture<R> future) {
        int code = response.errorCode();
        var category = classifyError(code);

        if (category == BotErrorCategory.CLIENT_ERROR
                || category == BotErrorCategory.USER_BLOCKED) {
            log.warn("Non-retryable Telegram error: "
                    + "code={} desc={}",
                    code, response.description());
            future.complete(response);
            return;
        }

        int maxAttempts = retryProperties.maxAttempts();
        if (attempt >= maxAttempts - 1) {
            future.complete(response);
            return;
        }

        long delayMs;
        if (category == BotErrorCategory.RATE_LIMITED) {
            delayMs = extractRetryAfter(response);
        } else {
            delayMs = backoffMs(attempt);
        }

        scheduleRetry(request, attempt + 1, delayMs, future);
    }

    private <T extends BaseRequest<T, R>, R extends BaseResponse>
            void handleException(T request, IOException ex,
                    int attempt, CompletableFuture<R> future) {
        int maxAttempts = retryProperties.maxAttempts();
        if (attempt >= maxAttempts - 1) {
            future.completeExceptionally(ex);
            return;
        }
        long delayMs = backoffMs(attempt);
        scheduleRetry(request, attempt + 1, delayMs, future);
    }

    private <T extends BaseRequest<T, R>, R extends BaseResponse>
            void scheduleRetry(T request, int nextAttempt,
                    long delayMs,
                    CompletableFuture<R> future) {
        Executor delayed = CompletableFuture.delayedExecutor(
                delayMs, TimeUnit.MILLISECONDS, executor);
        CompletableFuture.runAsync(() -> { /* delay */ }, delayed)
                .thenCompose(_ ->
                        executeWithRetryAsync(request,
                                nextAttempt))
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        future.completeExceptionally(ex);
                    } else {
                        future.complete(r);
                    }
                });
    }

    private long backoffMs(int attempt) {
        List<java.time.Duration> intervals =
                retryProperties.backoffIntervals();
        if (attempt < intervals.size()) {
            return intervals.get(attempt).toMillis();
        }
        return intervals.getLast().toMillis();
    }

    private long extractRetryAfter(BaseResponse response) {
        if (response.parameters() != null
                && response.parameters().retryAfter() != null) {
            return response.parameters().retryAfter() * 1000L;
        }
        return backoffMs(0);
    }

    /** Classifies a Telegram API error code. */
    static BotErrorCategory classifyError(int code) {
        if (code == 429) {
            return BotErrorCategory.RATE_LIMITED;
        }
        if (code == 403) {
            return BotErrorCategory.USER_BLOCKED;
        }
        if (code >= 400 && code < 500) {
            return BotErrorCategory.CLIENT_ERROR;
        }
        if (code >= 500) {
            return BotErrorCategory.SERVER_ERROR;
        }
        return BotErrorCategory.UNKNOWN;
    }
}

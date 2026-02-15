package com.advertmarket.communication.bot.internal.sender;

import static com.advertmarket.communication.bot.internal.BotConstants.METRIC_API_CALL;

import com.advertmarket.communication.bot.internal.error.BotErrorCategory;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Sends messages via the Telegram Bot API with rate limiting,
 * circuit breaking, and async retry logic.
 */
@Slf4j
@Component
public class TelegramSender {

    private static final String CANT_PARSE_ENTITIES =
            "can't parse entities";
    private static final int MAX_LOG_TEXT_LEN = 200;
    private static final int SHA256_LOG_HEX_CHARS = 12;
    private static final int HEX_BYTE_MASK = 0xFF;
    private static final int HEX_NIBBLE_SHIFT = 4;
    private static final int HEX_NIBBLE_MASK = 0x0F;
    private static final int STRING_BUILDER_EXTRA_CAPACITY = 32;
    private static final char[] HEX =
            "0123456789abcdef".toCharArray();

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

    /** Sends a MarkdownV2 text message to the given chat. */
    public void send(long chatId, @NonNull String text) {
        var request = new SendMessage(chatId, text)
                .parseMode(ParseMode.MarkdownV2);
        execute(request, chatId);
    }

    /** Sends a MarkdownV2 message with an inline keyboard. */
    public void send(long chatId, @NonNull String text,
            @NonNull InlineKeyboardMarkup keyboard) {
        var request = new SendMessage(chatId, text)
                .parseMode(ParseMode.MarkdownV2)
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

        if (isMarkdownV2ParseError(request, code, response)) {
            handleMarkdownV2ParseError(
                    (SendMessage) request, response, future);
            return;
        }

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

    private static <T extends BaseRequest<T, R>,
            R extends BaseResponse> boolean isMarkdownV2ParseError(
                    T request, int code, R response) {
        if (!(request instanceof SendMessage sendMessage)) {
            return false;
        }
        if (code != HttpStatus.BAD_REQUEST.value()) {
            return false;
        }
        if (sendMessage.getParseMode() != ParseMode.MarkdownV2) {
            return false;
        }
        String desc = response.description();
        return desc != null && desc.contains(CANT_PARSE_ENTITIES);
    }

    private <R extends BaseResponse> void handleMarkdownV2ParseError(
            SendMessage original, R originalResponse,
            CompletableFuture<R> future) {
        Long chatId = original.getChatId();
        String channelUsername = original.getChannelUsername();
        String text = original.getText();

        log.warn("Telegram rejected MarkdownV2 message (falling back to plain text): "
                        + "chat_id={} channel={} desc={} text_len={} text_sha256={}",
                chatId, channelUsername,
                sanitizeForLog(originalResponse.description()),
                safeLen(text),
                sha256Hex12(text));
        if (log.isDebugEnabled()) {
            log.debug("MarkdownV2 rejected message redacted_text={}",
                    redactForLog(text));
        }

        SendMessage fallback;
        if (chatId != null) {
            fallback = new SendMessage(chatId, text);
        } else if (channelUsername != null) {
            fallback = new SendMessage(channelUsername, text);
        } else {
            future.complete(originalResponse);
            return;
        }

        Keyboard keyboard = original.getReplyMarkup();
        if (keyboard != null) {
            fallback.replyMarkup(keyboard);
        }
        var linkPreview = original.getLinkPreviewOptions();
        if (linkPreview != null) {
            fallback.linkPreviewOptions(linkPreview);
        }

        bot.execute(fallback, new Callback<SendMessage,
                SendResponse>() {
            @Override
            public void onResponse(SendMessage req,
                    SendResponse response) {
                metrics.incrementCounter(METRIC_API_CALL,
                        "ok", String.valueOf(response.isOk()));
                future.complete((R) response);
            }

            @Override
            public void onFailure(SendMessage req,
                    IOException e) {
                metrics.incrementCounter(METRIC_API_CALL,
                        "ok", "false");
                future.complete(originalResponse);
            }
        });
    }

    private static int safeLen(String text) {
        return text == null ? 0 : text.length();
    }

    private static String sha256Hex12(String text) {
        if (text == null) {
            return "<null>";
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // Should not happen on a supported JDK, but keep logs stable if it does.
            return "<sha256-unavailable>";
        }
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return toHex(hash, SHA256_LOG_HEX_CHARS);
    }

    private static String toHex(byte[] bytes, int maxChars) {
        int n = Math.min(bytes.length * 2, maxChars);
        var out = new char[n];
        for (int i = 0; i < n / 2; i++) {
            int v = bytes[i] & HEX_BYTE_MASK;
            out[i * 2] = HEX[v >>> HEX_NIBBLE_SHIFT];
            out[i * 2 + 1] = HEX[v & HEX_NIBBLE_MASK];
        }
        return new String(out);
    }

    private static String sanitizeForLog(String text) {
        if (text == null) {
            return "<null>";
        }
        String normalized = text
                .replace("\r", "\\\\r")
                .replace("\n", "\\\\n");
        if (normalized.length() <= MAX_LOG_TEXT_LEN) {
            return normalized;
        }
        return normalized.substring(0, MAX_LOG_TEXT_LEN)
                + "...(len=" + normalized.length() + ")";
    }

    private static String redactForLog(String text) {
        if (text == null) {
            return "<null>";
        }
        String normalized = text
                .replace("\r", "\\\\r")
                .replace("\n", "\\\\n");
        int limit = Math.min(normalized.length(), MAX_LOG_TEXT_LEN);
        var out = new StringBuilder(limit + STRING_BUILDER_EXTRA_CAPACITY);
        for (int i = 0; i < limit; i++) {
            char c = normalized.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                out.append('*');
            } else {
                out.append(c);
            }
        }
        if (normalized.length() > MAX_LOG_TEXT_LEN) {
            out.append("...(len=").append(normalized.length()).append(')');
        }
        return out.toString();
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
        List<Duration> intervals =
                retryProperties.backoffIntervals();
        if (attempt < intervals.size()) {
            return intervals.get(attempt).toMillis();
        }
        return intervals.getLast().toMillis();
    }

    private long extractRetryAfter(BaseResponse response) {
        if (response.parameters() != null
                && response.parameters().retryAfter() != null) {
            return TimeUnit.SECONDS.toMillis(
                    response.parameters().retryAfter());
        }
        return backoffMs(0);
    }

    /** Classifies a Telegram API error code. */
    static BotErrorCategory classifyError(int code) {
        if (code == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return BotErrorCategory.RATE_LIMITED;
        }
        if (code == HttpStatus.FORBIDDEN.value()) {
            return BotErrorCategory.USER_BLOCKED;
        }
        if (code >= HttpStatus.BAD_REQUEST.value()
                && code < HttpStatus.INTERNAL_SERVER_ERROR
                        .value()) {
            return BotErrorCategory.CLIENT_ERROR;
        }
        if (code >= HttpStatus.INTERNAL_SERVER_ERROR
                .value()) {
            return BotErrorCategory.SERVER_ERROR;
        }
        return BotErrorCategory.UNKNOWN;
    }
}

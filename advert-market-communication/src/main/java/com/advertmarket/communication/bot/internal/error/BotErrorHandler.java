package com.advertmarket.communication.bot.internal.error;

import static com.advertmarket.communication.bot.internal.BotConstants.METRIC_HANDLER_ERROR;

import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

/**
 * Handles errors that occur during bot command processing.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class BotErrorHandler {

    private final MetricsFacade metrics;

    /**
     * Handles an exception thrown during command processing.
     *
     * @param exception the exception that occurred
     * @param updateId  the Telegram update id for correlation
     */
    public void handle(Exception exception, int updateId) {
        var category = classify(exception);
        metrics.incrementCounter(METRIC_HANDLER_ERROR,
                "category", category.name());
        log.error("Bot handler error update_id={} category={}",
                updateId, category, exception);
    }

    /**
     * Classifies an exception into an error category.
     *
     * <p>Attempts to use the Telegram error code from a
     * {@link BaseResponse} if available, falls back to
     * message-based classification.
     */
    static BotErrorCategory classify(Exception exception) {
        Integer code = extractErrorCode(exception);
        if (code != null) {
            return classifyByCode(code);
        }
        return classifyByMessage(exception.getMessage());
    }

    @Nullable
    private static Integer extractErrorCode(Exception ex) {
        // Pengrad wraps Telegram errors in a generic exception
        // that carries the response. Try to extract via message.
        String msg = ex.getMessage();
        if (msg == null) {
            return null;
        }
        // Check for common code patterns in the message
        if (msg.contains("429")) {
            return 429;
        }
        if (msg.contains("403")) {
            return 403;
        }
        if (msg.contains("400")) {
            return 400;
        }
        return null;
    }

    private static BotErrorCategory classifyByCode(int code) {
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

    private static BotErrorCategory classifyByMessage(
            String message) {
        if (message == null) {
            return BotErrorCategory.UNKNOWN;
        }
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("rate limit")) {
            return BotErrorCategory.RATE_LIMITED;
        }
        if (lower.contains("forbidden")
                || lower.contains("blocked")) {
            return BotErrorCategory.USER_BLOCKED;
        }
        if (lower.contains("bad request")) {
            return BotErrorCategory.CLIENT_ERROR;
        }
        return BotErrorCategory.UNKNOWN;
    }
}

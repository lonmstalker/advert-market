package com.advertmarket.communication.bot.internal.error;

import static com.advertmarket.communication.bot.internal.BotConstants.METRIC_HANDLER_ERROR;

import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.response.BaseResponse;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Handles errors that occur during bot command processing.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class BotErrorHandler {

    private final MetricsFacade metrics;
    private final TelegramSender sender;
    private final LocalizationService i18n;

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
     * Handles an exception and sends an i18n error message
     * to the user.
     *
     * @param exception the exception that occurred
     * @param updateId  the Telegram update id for correlation
     * @param userId    the Telegram user id to notify
     * @param lang      user language code, may be null
     */
    public void handleAndNotify(Exception exception, int updateId,
            long userId, @Nullable String lang) {
        handle(exception, updateId);
        trySendErrorMessage(userId, lang);
    }

    // CHECKSTYLE.OFF: IllegalCatch
    private void trySendErrorMessage(long userId,
            @Nullable String lang) {
        try {
            String msg = i18n.msg("bot.error", lang);
            sender.send(userId, msg);
        } catch (Exception e) {
            log.debug("Could not send error message to user={}",
                    userId, e);
        }
    }
    // CHECKSTYLE.ON: IllegalCatch

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
        if (msg.contains(String.valueOf(
                HttpStatus.TOO_MANY_REQUESTS.value()))) {
            return HttpStatus.TOO_MANY_REQUESTS.value();
        }
        if (msg.contains(String.valueOf(
                HttpStatus.FORBIDDEN.value()))) {
            return HttpStatus.FORBIDDEN.value();
        }
        if (msg.contains(String.valueOf(
                HttpStatus.BAD_REQUEST.value()))) {
            return HttpStatus.BAD_REQUEST.value();
        }
        return null;
    }

    private static BotErrorCategory classifyByCode(int code) {
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

    private static BotErrorCategory classifyByMessage(
            String message) {
        if (message == null) {
            return BotErrorCategory.UNKNOWN;
        }
        String lower = message.toLowerCase(Locale.ROOT);
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

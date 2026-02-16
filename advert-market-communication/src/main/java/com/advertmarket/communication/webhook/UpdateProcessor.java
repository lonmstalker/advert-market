package com.advertmarket.communication.webhook;

import static com.advertmarket.communication.bot.internal.BotConstants.MDC_CANARY;
import static com.advertmarket.communication.bot.internal.BotConstants.MDC_UPDATE_ID;
import static com.advertmarket.communication.bot.internal.BotConstants.MDC_USER_ID;
import static com.advertmarket.communication.bot.internal.BotConstants.METRIC_HANDLER_ERRORS;

import com.advertmarket.communication.bot.internal.dispatch.BotDispatcher;
import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.error.BotErrorHandler;
import com.advertmarket.communication.canary.CanaryRouter;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Async processor for Telegram updates.
 * Extracts user key, routes through canary, then delegates
 * to handler.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class UpdateProcessor {

    private final @NonNull CanaryRouter canaryRouter;
    private final @NonNull BotDispatcher dispatcher;
    private final @NonNull BotErrorHandler errorHandler;
    private final @NonNull MetricsFacade metrics;

    private final @NonNull ExecutorService botUpdateExecutor;

    /**
     * Submit update for async processing. Returns immediately.
     *
     * @param update the Telegram update
     */
    public void processAsync(Update update) {
        try {
            botUpdateExecutor.submit(
                    () -> processUpdate(update)).isDone();
        } catch (RejectedExecutionException ex) {
            metrics.incrementCounter(
                    MetricNames.WEBHOOK_DISPATCH_REJECTED,
                    "reason", "rejected_execution");
            log.warn("Bot update dispatch rejected for update_id={}",
                    update.updateId(), ex);
        }
    }

    private void processUpdate(Update update) {
        long userId = extractUserId(update);
        try {
            MDC.put(MDC_USER_ID,
                    String.valueOf(userId));
            MDC.put(MDC_UPDATE_ID,
                    String.valueOf(update.updateId()));

            boolean canary = canaryRouter.isCanary(userId);
            MDC.put(MDC_CANARY,
                    String.valueOf(canary));

            log.info("Processing update_id={} user_id={} "
                    + "canary={}",
                    update.updateId(), userId, canary);

            dispatcher.dispatch(new UpdateContext(update));

        } catch (Exception e) {
            metrics.incrementCounter(METRIC_HANDLER_ERRORS);
            log.error("Error processing update_id={}",
                    update.updateId(), e);
            errorHandler.handleAndNotify(e,
                    update.updateId(), userId,
                    extractLangCode(update));
        } finally {
            MDC.clear();
        }
    }

    /**
     * Extract a stable user key from the update.
     *
     * @param update the Telegram update
     * @return user id or fallback to update_id
     */
    static long extractUserId(Update update) {
        User user = extractUser(update);
        if (user != null) {
            return user.id();
        }
        if (update.message() != null
                && update.message().chat() != null) {
            return update.message().chat().id();
        }
        return update.updateId();
    }

    /**
     * Extract user language code from the update.
     *
     * @param update the Telegram update
     * @return language code or null if unavailable
     */
    @Nullable
    static String extractLangCode(Update update) {
        User user = extractUser(update);
        return user != null ? user.languageCode() : null;
    }

    // CHECKSTYLE.OFF: CyclomaticComplexity|NPathComplexity
    @Nullable
    private static User extractUser(Update update) {
        if (update.message() != null
                && update.message().from() != null) {
            return update.message().from();
        }
        if (update.callbackQuery() != null
                && update.callbackQuery().from() != null) {
            return update.callbackQuery().from();
        }
        if (update.inlineQuery() != null
                && update.inlineQuery().from() != null) {
            return update.inlineQuery().from();
        }
        if (update.myChatMember() != null
                && update.myChatMember().from() != null) {
            return update.myChatMember().from();
        }
        if (update.chatMember() != null
                && update.chatMember().from() != null) {
            return update.chatMember().from();
        }
        return null;
    }
    // CHECKSTYLE.ON: CyclomaticComplexity|NPathComplexity
}

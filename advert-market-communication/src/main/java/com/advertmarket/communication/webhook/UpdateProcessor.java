package com.advertmarket.communication.webhook;

import static com.advertmarket.communication.bot.internal.BotConstants.MDC_CANARY;
import static com.advertmarket.communication.bot.internal.BotConstants.MDC_UPDATE_ID;
import static com.advertmarket.communication.bot.internal.BotConstants.MDC_USER_ID;
import static com.advertmarket.communication.bot.internal.BotConstants.METRIC_HANDLER_ERRORS;

import com.advertmarket.communication.bot.internal.builder.MarkdownV2Util;
import com.advertmarket.communication.bot.internal.dispatch.BotDispatcher;
import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.communication.canary.CanaryRouter;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.model.Update;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Async processor for Telegram updates.
 * Extracts user key, routes through canary, then delegates
 * to handler.
 */
@Slf4j
@Component
public class UpdateProcessor {

    private final CanaryRouter canaryRouter;
    private final BotDispatcher dispatcher;
    private final TelegramSender sender;
    private final MetricsFacade metrics;
    private final ExecutorService executor;

    /** Creates the update processor with canary routing. */
    public UpdateProcessor(CanaryRouter canaryRouter,
            BotDispatcher dispatcher,
            TelegramSender sender,
            MetricsFacade metrics,
            @Qualifier("botUpdateExecutor")
            ExecutorService executor) {
        this.canaryRouter = canaryRouter;
        this.dispatcher = dispatcher;
        this.sender = sender;
        this.metrics = metrics;
        this.executor = executor;
    }

    /**
     * Submit update for async processing. Returns immediately.
     *
     * @param update the Telegram update
     */
    public void processAsync(Update update) {
        executor.submit(() -> processUpdate(update));
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
            trySendError(userId, update.updateId());
        } finally {
            MDC.clear();
        }
    }

    private void trySendError(long userId, int updateId) {
        try {
            sender.send(userId,
                    MarkdownV2Util.escape(
                            "⚠ An error occurred. "
                            + "Please try again later."));
        } catch (Exception ex) {
            log.debug("Could not send error message for "
                    + "update_id={}", updateId, ex);
        }
    }

    /**
     * Extract a stable user key from the update.
     *
     * @param update the Telegram update
     * @return user id or fallback to update_id
     */
    // CHECKSTYLE.OFF: CyclomaticComplexity|NPathComplexity
    static long extractUserId(Update update) {
        if (update.message() != null
                && update.message().from() != null) {
            return update.message().from().id();
        }
        if (update.callbackQuery() != null
                && update.callbackQuery().from() != null) {
            return update.callbackQuery().from().id();
        }
        if (update.inlineQuery() != null
                && update.inlineQuery().from() != null) {
            return update.inlineQuery().from().id();
        }
        if (update.myChatMember() != null
                && update.myChatMember().from() != null) {
            return update.myChatMember().from().id();
        }
        if (update.chatMember() != null
                && update.chatMember().from() != null) {
            return update.chatMember().from().id();
        }
        if (update.message() != null
                && update.message().chat() != null) {
            return update.message().chat().id();
        }
        return update.updateId();
    }
    // CHECKSTYLE.ON: CyclomaticComplexity|NPathComplexity
}

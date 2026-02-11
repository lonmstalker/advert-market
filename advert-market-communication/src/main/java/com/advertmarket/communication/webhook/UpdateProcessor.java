package com.advertmarket.communication.webhook;

import static com.advertmarket.communication.bot.internal.BotConstants.MDC_CANARY;
import static com.advertmarket.communication.bot.internal.BotConstants.MDC_UPDATE_ID;
import static com.advertmarket.communication.bot.internal.BotConstants.MDC_USER_ID;
import static com.advertmarket.communication.bot.internal.BotConstants.METRIC_HANDLER_ERRORS;

import com.advertmarket.communication.bot.internal.dispatch.BotDispatcher;
import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.communication.canary.CanaryRouter;
import com.pengrad.telegrambot.model.Update;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
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
    private final Counter handlerErrors;
    private final ExecutorService executor;

    /** Creates the update processor with canary routing. */
    public UpdateProcessor(CanaryRouter canaryRouter,
            BotDispatcher dispatcher,
            TelegramSender sender,
            MeterRegistry meterRegistry,
            @Qualifier("botUpdateExecutor")
            ExecutorService executor) {
        this.canaryRouter = canaryRouter;
        this.dispatcher = dispatcher;
        this.sender = sender;
        this.handlerErrors = Counter.builder(METRIC_HANDLER_ERRORS)
                .description("Update handler errors")
                .register(meterRegistry);
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
            org.slf4j.MDC.put(MDC_USER_ID,
                    String.valueOf(userId));
            org.slf4j.MDC.put(MDC_UPDATE_ID,
                    String.valueOf(update.updateId()));

            boolean canary = canaryRouter.isCanary(userId);
            org.slf4j.MDC.put(MDC_CANARY,
                    String.valueOf(canary));

            log.info("Processing update_id={} user_id={} "
                    + "canary={}",
                    update.updateId(), userId, canary);

            dispatcher.dispatch(new UpdateContext(update));

        } catch (Exception e) {
            handlerErrors.increment();
            log.error("Error processing update_id={}",
                    update.updateId(), e);
            trySendError(userId, update.updateId());
        } finally {
            org.slf4j.MDC.clear();
        }
    }

    private void trySendError(long userId, int updateId) {
        try {
            sender.send(userId,
                    "\u26a0 An error occurred. " // NON-NLS
                    + "Please try again later.");
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
}

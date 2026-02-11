package com.advertmarket.communication.webhook;

import com.advertmarket.communication.canary.CanaryRouter;
import com.pengrad.telegrambot.model.Update;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Async processor for Telegram updates.
 * Extracts user key, routes through canary, then delegates to handler.
 */
@Component
public class UpdateProcessor {

    private static final Logger log = LoggerFactory.getLogger(UpdateProcessor.class);

    private final CanaryRouter canaryRouter;
    private final Counter handlerErrors;
    private final ExecutorService executor;

    public UpdateProcessor(CanaryRouter canaryRouter, MeterRegistry meterRegistry) {
        this.canaryRouter = canaryRouter;
        this.handlerErrors = Counter.builder("telegram.handler.errors")
                .description("Update handler errors")
                .register(meterRegistry);
        // Virtual thread executor for async processing
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Submit update for async processing. Returns immediately.
     */
    public void processAsync(Update update) {
        executor.submit(() -> processUpdate(update));
    }

    private void processUpdate(Update update) {
        long userId = extractUserId(update);
        try {
            MDC.put("user_id", String.valueOf(userId));
            MDC.put("update_id", String.valueOf(update.updateId()));

            boolean canary = canaryRouter.isCanary(userId);
            MDC.put("canary", String.valueOf(canary));

            log.info("Processing update_id={} user_id={} canary={}", update.updateId(), userId, canary);

            // TODO: Delegate to actual BotUpdateHandler based on canary flag
            // if (canary) { canaryHandler.handle(update); }
            // else { stableHandler.handle(update); }

        } catch (Exception e) {
            handlerErrors.increment();
            log.error("Error processing update_id={}", update.updateId(), e);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Extract a stable user key from the update.
     * Priority: message.from.id → callback_query.from.id → inline_query.from.id
     * → my_chat_member.from.id → chat_member.from.id → chat.id → update_id
     */
    static long extractUserId(Update update) {
        if (update.message() != null && update.message().from() != null) {
            return update.message().from().id();
        }
        if (update.callbackQuery() != null && update.callbackQuery().from() != null) {
            return update.callbackQuery().from().id();
        }
        if (update.inlineQuery() != null && update.inlineQuery().from() != null) {
            return update.inlineQuery().from().id();
        }
        if (update.myChatMember() != null && update.myChatMember().from() != null) {
            return update.myChatMember().from().id();
        }
        if (update.chatMember() != null && update.chatMember().from() != null) {
            return update.chatMember().from().id();
        }
        if (update.message() != null && update.message().chat() != null) {
            return update.message().chat().id();
        }
        // Last resort: use update_id (not sticky across updates, documented limitation)
        return update.updateId();
    }
}
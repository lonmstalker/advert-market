package com.advertmarket.communication.bot.internal.dispatch;

import com.advertmarket.communication.bot.internal.sender.TelegramSender;

/**
 * Contract for handling non-command text messages and media.
 *
 * <p>Implementations are discovered by {@link BotDispatcher} and
 * evaluated in {@link #order()} priority (lower = higher priority).
 */
public interface MessageHandler {

    /**
     * Returns true if this handler can process the given update.
     *
     * @param ctx the update context
     * @return true if this handler should process the update
     */
    boolean canHandle(UpdateContext ctx);

    /**
     * Handles the update.
     *
     * @param ctx    the update context
     * @param sender the sender for replying
     */
    void handle(UpdateContext ctx, TelegramSender sender);

    /**
     * Returns the evaluation order. Lower values have higher priority.
     *
     * @return the order value (default 0)
     */
    default int order() {
        return 0;
    }
}

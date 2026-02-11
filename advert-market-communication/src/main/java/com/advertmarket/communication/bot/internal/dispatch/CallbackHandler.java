package com.advertmarket.communication.bot.internal.dispatch;

import com.advertmarket.communication.bot.internal.sender.TelegramSender;

/**
 * Contract for a Telegram callback query handler.
 *
 * <p>Implementations are discovered by {@link BotDispatcher} and matched
 * by the {@link #prefix()} of the callback data. Longer prefixes take
 * priority over shorter ones.
 */
public interface CallbackHandler {

    /**
     * Returns the prefix to match against callback data (e.g. "lang:").
     *
     * @return the callback data prefix
     */
    String prefix();

    /**
     * Handles a callback query whose data starts with this handler's prefix.
     *
     * @param ctx    the update context
     * @param sender the sender for replying
     */
    void handle(UpdateContext ctx, TelegramSender sender);
}

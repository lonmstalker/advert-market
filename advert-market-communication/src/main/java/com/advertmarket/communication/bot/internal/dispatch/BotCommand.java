package com.advertmarket.communication.bot.internal.dispatch;

import com.advertmarket.communication.bot.internal.sender.TelegramSender;

/**
 * Contract for a Telegram bot command handler.
 *
 * <p>Implementations are discovered by {@link BotDispatcher} via Spring
 * injection and matched by the {@link #command()} prefix.
 */
public interface BotCommand {

    /**
     * Returns the command string including the leading slash (e.g. "/start").
     *
     * @return the command trigger
     */
    String command();

    /**
     * Handles an incoming update that matches this command.
     *
     * @param ctx    the update context
     * @param sender the sender for replying
     */
    void handle(UpdateContext ctx, TelegramSender sender);
}

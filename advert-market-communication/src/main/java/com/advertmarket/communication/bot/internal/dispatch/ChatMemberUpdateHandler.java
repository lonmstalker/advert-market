package com.advertmarket.communication.bot.internal.dispatch;

import com.pengrad.telegrambot.model.ChatMemberUpdated;

/**
 * Contract for handling {@code my_chat_member} updates
 * (bot membership changes in chats/channels).
 *
 * <p>Unlike {@link CallbackHandler} and {@link MessageHandler},
 * this handler receives {@link ChatMemberUpdated} directly because
 * these are system events, not user dialogs.
 */
public interface ChatMemberUpdateHandler {

    /**
     * Returns true if this handler can process the given update.
     *
     * @param update the chat member update
     * @return true if this handler should process the update
     */
    boolean canHandle(ChatMemberUpdated update);

    /**
     * Handles the chat member update.
     *
     * @param update the chat member update
     */
    void handle(ChatMemberUpdated update);
}
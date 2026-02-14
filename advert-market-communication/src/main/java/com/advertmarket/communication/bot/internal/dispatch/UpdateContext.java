package com.advertmarket.communication.bot.internal.dispatch;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.ChatMemberUpdated;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Convenience wrapper around a Telegram {@link Update}.
 *
 * @param update the raw Telegram update
 */
public record UpdateContext(Update update) {

    /** Returns the chat id extracted from this update. */
    public long chatId() {
        if (update.message() != null && update.message().chat() != null) {
            return update.message().chat().id();
        }
        if (update.callbackQuery() != null
                && update.callbackQuery().maybeInaccessibleMessage() != null
                && update.callbackQuery().maybeInaccessibleMessage().chat() != null) {
            return update.callbackQuery().maybeInaccessibleMessage().chat().id();
        }
        return userId();
    }

    /** Sentinel value when no user can be extracted from the update. */
    public static final long UNKNOWN_USER_ID = -1L;

    /** Returns the user id extracted from this update. */
    public long userId() {
        var user = user();
        return user != null ? user.id() : UNKNOWN_USER_ID;
    }

    /** Returns the {@link User} from this update, or null. */
    @Nullable
    public User user() {
        if (update.message() != null && update.message().from() != null) {
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
        return null;
    }

    /** Returns the message text, or null. */
    @Nullable
    public String messageText() {
        return update.message() != null ? update.message().text() : null;
    }

    /** Returns the callback query, or null. */
    @Nullable
    public CallbackQuery callbackQuery() {
        return update.callbackQuery();
    }

    /** Returns the callback data string, or null. */
    @Nullable
    public String callbackData() {
        return update.callbackQuery() != null
                ? update.callbackQuery().data() : null;
    }

    /** Returns true if this update contains a text message. */
    public boolean isTextMessage() {
        return update.message() != null && update.message().text() != null;
    }

    /** Returns true if this update is a callback query. */
    public boolean isCallbackQuery() {
        return update.callbackQuery() != null;
    }

    /** Returns true if this update is a my_chat_member update. */
    public boolean isMyChatMemberUpdate() {
        return update.myChatMember() != null;
    }

    /** Returns the my_chat_member update, or null. */
    @Nullable
    public ChatMemberUpdated myChatMember() {
        return update.myChatMember();
    }

    /** Returns the user's language code from their Telegram profile. */
    @Nullable
    public String languageCode() {
        var user = user();
        return user != null ? user.languageCode() : null;
    }
}

package com.advertmarket.communication.bot.internal.error;

/**
 * Classification of Telegram Bot API errors.
 */
public enum BotErrorCategory {

    /** Client-side errors (400, 401). */
    CLIENT_ERROR,
    /** Rate limited by Telegram (429). */
    RATE_LIMITED,
    /** User blocked the bot or chat not found (403). */
    USER_BLOCKED,
    /** Telegram server errors (5xx). */
    SERVER_ERROR,
    /** Unclassified errors. */
    UNKNOWN
}

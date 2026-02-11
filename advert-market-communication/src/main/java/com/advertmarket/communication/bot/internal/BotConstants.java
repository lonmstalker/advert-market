package com.advertmarket.communication.bot.internal;

/**
 * Shared constants for the Telegram bot framework.
 */
public final class BotConstants {

    private BotConstants() {
    }

    // --- MDC keys ---

    /** MDC key for the Telegram user id. */
    public static final String MDC_USER_ID = "user_id";

    /** MDC key for the Telegram update id. */
    public static final String MDC_UPDATE_ID = "update_id";

    /** MDC key for canary routing flag. */
    public static final String MDC_CANARY = "canary";

    // --- Metric names ---

    /** Counter for Telegram API call outcomes. */
    public static final String METRIC_API_CALL = "telegram.api.call";

    /** Counter per handler error with category tag. */
    public static final String METRIC_HANDLER_ERROR =
            "telegram.handler.error";

    /** Counter for webhook-level errors. */
    public static final String METRIC_WEBHOOK_ERROR =
            "telegram.webhook.error";

    /** Counter for handler processing errors. */
    public static final String METRIC_HANDLER_ERRORS =
            "telegram.handler.errors";

    // --- Callback prefixes ---

    /** Callback data prefix for language selection. */
    public static final String CALLBACK_LANG = "lang:";
}

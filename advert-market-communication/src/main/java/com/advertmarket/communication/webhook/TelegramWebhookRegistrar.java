package com.advertmarket.communication.webhook;

import com.advertmarket.communication.bot.internal.config.TelegramBotProperties;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SetWebhook;
import com.pengrad.telegrambot.response.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Registers Telegram webhook automatically on application start.
 *
 * <p>This removes the manual "setWebhook" step from deployment.
 */
@Component
public class TelegramWebhookRegistrar {

    private static final Logger log =
            LoggerFactory.getLogger(TelegramWebhookRegistrar.class);

    private final TelegramBot bot;
    private final TelegramBotProperties botProperties;

    /**
     * Creates the registrar.
     *
     * @param bot Telegram Bot API client
     * @param botProperties bot configuration (token, webhook url, secret)
     */
    public TelegramWebhookRegistrar(
            TelegramBot bot,
            TelegramBotProperties botProperties
    ) {
        this.bot = bot;
        this.botProperties = botProperties;
    }

    /**
     * Registers webhook after the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        registerWebhook();
    }

    /**
     * Registers webhook if {@code app.telegram.webhook.url} is configured.
     */
    void registerWebhook() {
        String url = botProperties.webhook().url();
        if (!StringUtils.hasText(url)) {
            log.info("Telegram webhook URL is empty; skipping setWebhook");
            return;
        }

        BaseResponse response = bot.execute(new SetWebhook()
                .url(url)
                .secretToken(botProperties.webhook().secret())
                .allowedUpdates("message", "callback_query",
                        "my_chat_member")
        );

        if (!response.isOk()) {
            throw new IllegalStateException(
                    "Telegram setWebhook failed: "
                            + response.errorCode() + " "
                            + response.description()
            );
        }

        log.info("Telegram webhook registered: {}", url);
    }
}

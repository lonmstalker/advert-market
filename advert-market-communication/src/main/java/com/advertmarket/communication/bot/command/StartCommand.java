package com.advertmarket.communication.bot.command;

import com.advertmarket.communication.bot.internal.builder.KeyboardBuilder;
import com.advertmarket.communication.bot.internal.builder.Reply;
import com.advertmarket.communication.bot.internal.config.TelegramBotProperties;
import com.advertmarket.communication.bot.internal.dispatch.BotCommand;
import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.shared.i18n.LocalizationService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles the /start command with deep linking support.
 *
 * <p>Supported deep link prefixes: deal, channel, dispute, deposit.
 */
@Slf4j
@Component
public class StartCommand implements BotCommand {

    private static final Map<String, String> DEEP_LINK_ROUTES =
            Map.of(
                    "deal", "/deals/",
                    "channel", "/channels/",
                    "dispute", "/disputes/",
                    "deposit", "/deposit/"
            );

    private final String webAppUrl;
    private final LocalizationService i18n;

    /** Creates the start command. */
    public StartCommand(TelegramBotProperties botProperties,
            LocalizationService i18n) {
        this.webAppUrl = botProperties.webapp().url();
        this.i18n = i18n;
    }

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public void handle(UpdateContext ctx, TelegramSender sender) {
        String text = ctx.messageText();
        String param = extractDeepLinkParam(text);

        if (param != null) {
            handleDeepLink(ctx, sender, param);
        } else {
            handleWelcome(ctx, sender);
        }
        // TODO: upsert user via identity-api
    }

    private void handleWelcome(UpdateContext ctx,
            TelegramSender sender) {
        String lang = langOf(ctx);
        Reply.html(ctx, i18n.msg("bot.welcome", lang))
                .keyboard(KeyboardBuilder.inline()
                        .webAppButton(
                                i18n.msg("bot.welcome.button",
                                        lang),
                                webAppUrl)
                        .build())
                .send(sender);
    }

    private void handleDeepLink(UpdateContext ctx,
            TelegramSender sender, String param) {
        String lang = langOf(ctx);
        String route = resolveRoute(param);
        String url = webAppUrl + route;

        Reply.html(ctx,
                i18n.msg("bot.deeplink.opening", lang))
                .keyboard(KeyboardBuilder.inline()
                        .webAppButton(
                                i18n.msg("bot.deeplink.button",
                                        lang),
                                url)
                        .build())
                .disablePreview()
                .send(sender);
    }

    private static String langOf(UpdateContext ctx) {
        return ctx.languageCode() != null
                ? ctx.languageCode() : "ru";
    }

    static String extractDeepLinkParam(String text) {
        if (text == null || !text.contains(" ")) {
            return null;
        }
        return text.substring(text.indexOf(' ') + 1).trim();
    }

    static String resolveRoute(String param) {
        for (var entry : DEEP_LINK_ROUTES.entrySet()) {
            if (param.startsWith(entry.getKey() + "_")) {
                String id = param.substring(
                        entry.getKey().length() + 1);
                return entry.getValue() + id;
            }
        }
        return "/?ref=" + param;
    }
}

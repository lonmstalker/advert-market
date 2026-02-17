package com.advertmarket.communication.bot.command;

import com.advertmarket.communication.bot.internal.builder.KeyboardBuilder;
import com.advertmarket.communication.bot.internal.builder.Reply;
import com.advertmarket.communication.bot.internal.config.TelegramBotProperties;
import com.advertmarket.communication.bot.internal.dispatch.BotCommand;
import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.i18n.LocalizationService;
import com.pengrad.telegrambot.model.User;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Handles the /start command with deep linking support.
 *
 * <p>Supported deep link prefixes: deal, channel, dispute, deposit.
 */
@Slf4j
@Component
public class StartCommand implements BotCommand {

    private static final Pattern SAFE_PARAM =
            Pattern.compile("[a-zA-Z0-9_-]+");

    private static final Map<String, String> DEEP_LINK_ROUTES =
            Map.of(
                    "deal", "/deals/",
                    "channel", "/catalog/channels/",
                    // Dedicated dispute/deposit pages are not implemented in SPA routes yet.
                    // Route both to deal details by id.
                    "dispute", "/deals/",
                    "deposit", "/deals/"
            );

    private final String webAppUrl;
    private final String welcomeCustomEmojiId;
    private final LocalizationService i18n;
    private final UserRepository userRepository;

    /** Creates the start command. */
    public StartCommand(TelegramBotProperties botProperties,
            LocalizationService i18n,
            UserRepository userRepository) {
        this.webAppUrl = botProperties.webapp().url();
        this.welcomeCustomEmojiId = botProperties.welcome() != null
                ? botProperties.welcome().customEmojiId() : "";
        this.i18n = i18n;
        this.userRepository = userRepository;
    }

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public void handle(UpdateContext ctx, TelegramSender sender) {
        upsertUser(ctx);

        String text = ctx.messageText();
        String param = extractDeepLinkParam(text);

        if (param != null) {
            handleDeepLink(ctx, sender, param);
        } else {
            handleWelcome(ctx, sender);
        }
    }

    private void upsertUser(UpdateContext ctx) {
        User user = ctx.user();
        if (user == null) {
            return;
        }
        var data = new TelegramUserData(
                user.id(),
                user.firstName(),
                user.lastName(),
                user.username(),
                user.languageCode());
        userRepository.upsert(data);
    }

    private void handleWelcome(UpdateContext ctx,
            TelegramSender sender) {
        String lang = langOf(ctx);
        String welcome = i18n.msg("bot.welcome", lang);
        Reply.text(ctx, maybePrefixCustomEmoji(welcome,
                        welcomeCustomEmojiId))
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

        Reply.text(ctx,
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

    static String maybePrefixCustomEmoji(
            String message, String customEmojiId) {
        if (StringUtils.isBlank(customEmojiId)) {
            return message;
        }
        if (!customEmojiId.chars().allMatch(Character::isDigit)) {
            return message;
        }
        // Telegram custom emoji in MarkdownV2:
        // ![⭐](tg://emoji?id=5368324170671202286)
        return "![⭐](tg://emoji?id=" + customEmojiId + ") "
                + message;
    }

    static String extractDeepLinkParam(String text) {
        if (text == null || !text.contains(" ")) {
            return null;
        }
        return text.substring(text.indexOf(' ') + 1).trim();
    }

    static String resolveRoute(String param) {
        if (!SAFE_PARAM.matcher(param).matches()) {
            return "/";
        }
        for (var entry : DEEP_LINK_ROUTES.entrySet()) {
            if (param.startsWith(entry.getKey() + "_")) {
                String id = param.substring(
                        entry.getKey().length() + 1);
                return entry.getValue() + id;
            }
        }
        return "/?ref=" + URLEncoder.encode(
                param, StandardCharsets.UTF_8);
    }
}

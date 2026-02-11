package com.advertmarket.communication.bot.command;

import static com.advertmarket.communication.bot.internal.BotConstants.CALLBACK_LANG;

import com.advertmarket.communication.bot.internal.builder.KeyboardBuilder;
import com.advertmarket.communication.bot.internal.builder.Reply;
import com.advertmarket.communication.bot.internal.dispatch.BotCommand;
import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.shared.i18n.LocalizationService;
import org.springframework.stereotype.Component;

/**
 * Handles the /language command to display language selection.
 */
@Component
public class LanguageCommand implements BotCommand {

    private final LocalizationService i18n;

    /** Creates the language command. */
    public LanguageCommand(LocalizationService i18n) {
        this.i18n = i18n;
    }

    @Override
    public String command() {
        return "/language";
    }

    @Override
    public void handle(UpdateContext ctx, TelegramSender sender) {
        String lang = ctx.languageCode() != null
                ? ctx.languageCode() : "ru";
        Reply.html(ctx,
                i18n.msg("bot.language.prompt", lang))
                .keyboard(KeyboardBuilder.inline()
                        .callbackButton(
                                i18n.msg("bot.language.ru", lang),
                                CALLBACK_LANG + "ru")
                        .callbackButton(
                                i18n.msg("bot.language.en", lang),
                                CALLBACK_LANG + "en")
                        .build())
                .send(sender);
    }
}

package com.advertmarket.communication.bot.command;

import static com.advertmarket.communication.bot.internal.BotConstants.CALLBACK_LANG;

import com.advertmarket.communication.bot.internal.builder.Reply;
import com.advertmarket.communication.bot.internal.dispatch.CallbackHandler;
import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.shared.i18n.LocalizationService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles callback queries with prefix "lang:" for language
 * selection.
 */
@Slf4j
@Component
public class LanguageCallbackHandler implements CallbackHandler {

    private static final Map<String, String> LANGUAGE_NAMES =
            Map.of(
                    "ru", "\u0420\u0443\u0441\u0441\u043a\u0438\u0439", // NON-NLS
                    "en", "English"
            );

    private final LocalizationService i18n;

    /** Creates the callback handler. */
    public LanguageCallbackHandler(LocalizationService i18n) {
        this.i18n = i18n;
    }

    @Override
    public String prefix() {
        return CALLBACK_LANG;
    }

    @Override
    public void handle(UpdateContext ctx, TelegramSender sender) {
        String data = ctx.callbackData();
        String langCode = data != null
                ? data.substring(prefix().length()) : "";
        String langName = LANGUAGE_NAMES.getOrDefault(
                langCode, langCode);

        log.info("Language selected: {} for user_id={}",
                langCode, ctx.userId());

        String lang = ctx.languageCode() != null
                ? ctx.languageCode() : "ru";
        Reply.callback(ctx)
                .text(i18n.msg("bot.language.selected",
                        lang, langName))
                .send(sender);
        // TODO: save language preference via identity-api
    }
}

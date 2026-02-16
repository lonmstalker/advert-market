package com.advertmarket.communication.bot.command;

import static com.advertmarket.communication.bot.internal.BotConstants.CALLBACK_LANG;

import com.advertmarket.communication.bot.internal.builder.Reply;
import com.advertmarket.communication.bot.internal.dispatch.CallbackHandler;
import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.model.UserId;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles callback queries with prefix "lang:" for language
 * selection.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class LanguageCallbackHandler implements CallbackHandler {

    private static final Map<String, String> LANGUAGE_NAMES =
            Map.of(
                    "ru", "Русский", // NON-NLS
                    "en", "English"
            );

    private final LocalizationService i18n;
    private final UserRepository userRepository;

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

        saveLanguagePreference(ctx, langCode);

        String lang = ctx.languageCode() != null
                ? ctx.languageCode() : "ru";
        Reply.callback(ctx)
                .callbackText(i18n.msg("bot.language.selected",
                        lang, langName))
                .send(sender);
    }

    private void saveLanguagePreference(UpdateContext ctx,
            String langCode) {
        if (ctx.user() == null || langCode.isEmpty()) {
            return;
        }
        userRepository.updateLanguage(
                new UserId(ctx.userId()), langCode);
    }
}

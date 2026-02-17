package com.advertmarket.communication.notification;

import com.advertmarket.communication.api.notification.NotificationPort;
import com.advertmarket.communication.api.notification.NotificationRequest;
import com.advertmarket.communication.bot.internal.builder.MarkdownV2Util;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.model.UserId;
import com.pengrad.telegrambot.response.SendResponse;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sends notifications to users via Telegram messages
 * using localized templates from message bundles.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class TelegramNotificationService
        implements NotificationPort {

    private static final String DEFAULT_LOCALE = "en";

    private final TelegramSender sender;
    private final LocalizationService i18n;
    private final UserRepository userRepository;

    @Override
    public boolean send(NotificationRequest request) {
        try {
            String key = "notification."
                    + request.type().name()
                            .toLowerCase(Locale.ROOT);
            String locale = resolveLocale(request.recipientUserId());
            String template = i18n.msg(key, locale);
            String rendered = substituteVariables(
                    template, request.variables());
            SendResponse response =
                    sender.send(request.recipientUserId(), rendered);
            if (response.isOk()) {
                return true;
            }
            log.warn("Telegram API rejected notification type={} to user={} code={} description={}",
                    request.type(),
                    request.recipientUserId(),
                    response.errorCode(),
                    response.description());
            return false;
        } catch (Exception e) {
            log.error("Failed to send notification type={} "
                    + "to user={}", request.type(),
                    request.recipientUserId(), e);
            return false;
        }
    }

    private String resolveLocale(long recipientUserId) {
        try {
            return userRepository.findById(new UserId(recipientUserId))
                    .map(user -> user.languageCode().trim())
                    .filter(lang -> !lang.isEmpty())
                    .orElse(DEFAULT_LOCALE);
        } catch (IllegalArgumentException ex) {
            log.debug("Invalid recipient id for locale lookup: {}",
                    recipientUserId, ex);
            return DEFAULT_LOCALE;
        }
    }

    private String substituteVariables(String template,
            Map<String, String> variables) {
        String escaped = MarkdownV2Util.escape(template);
        for (var entry : variables.entrySet()) {
            String escapedKey = MarkdownV2Util.escape(
                    "{" + entry.getKey() + "}");
            escaped = escaped.replace(
                    escapedKey,
                    MarkdownV2Util.escape(entry.getValue()));
        }
        return escaped;
    }
}

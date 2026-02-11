package com.advertmarket.communication.notification;

import com.advertmarket.communication.api.notification.NotificationPort;
import com.advertmarket.communication.api.notification.NotificationRequest;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.shared.i18n.LocalizationService;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sends notifications to users via Telegram messages
 * using localized templates from message bundles.
 */
@Slf4j
@Component
public class TelegramNotificationService
        implements NotificationPort {

    private final TelegramSender sender;
    private final LocalizationService i18n;

    /** Creates the notification service. */
    public TelegramNotificationService(TelegramSender sender,
            LocalizationService i18n) {
        this.sender = sender;
        this.i18n = i18n;
    }

    @Override
    public boolean send(NotificationRequest request) {
        try {
            String key = "notification."
                    + request.type().name()
                            .toLowerCase(Locale.ROOT);
            // Default to Russian until user locale is stored
            String template = i18n.msg(key, "ru");
            String rendered = substituteVariables(
                    template, request.variables());
            sender.send(request.recipientUserId(), rendered);
            return true;
        } catch (Exception e) {
            log.error("Failed to send notification type={} "
                    + "to user={}", request.type(),
                    request.recipientUserId(), e);
            return false;
        }
    }

    private String substituteVariables(String template,
            Map<String, String> variables) {
        String result = template;
        for (var entry : variables.entrySet()) {
            result = result.replace(
                    "{" + entry.getKey() + "}",
                    entry.getValue());
        }
        return result;
    }
}

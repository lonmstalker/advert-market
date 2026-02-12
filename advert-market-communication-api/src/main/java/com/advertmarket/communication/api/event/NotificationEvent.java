package com.advertmarket.communication.api.event;

import com.advertmarket.shared.event.DomainEvent;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Command to send a notification to a user.
 *
 * @param recipientId Telegram user ID of the recipient
 * @param template message template name
 * @param locale target locale code (e.g. "ru", "en")
 * @param vars template variable substitutions
 * @param buttons optional inline keyboard buttons
 */
public record NotificationEvent(
        long recipientId,
        @NonNull String template,
        @NonNull String locale,
        @NonNull Map<String, String> vars,
        @Nullable List<NotificationButton> buttons)
        implements DomainEvent {

    /**
     * Creates a notification event with defensive copies.
     *
     * @throws NullPointerException if required parameters are null
     */
    public NotificationEvent {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(vars, "vars");
        vars = Map.copyOf(vars);
        buttons = buttons != null ? List.copyOf(buttons) : null;
    }
}

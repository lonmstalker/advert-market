package com.advertmarket.communication.api.notification;

import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Request to send a notification to a user.
 *
 * @param recipientUserId Telegram user id of the recipient
 * @param type            notification type determining the template
 * @param variables       template variables (e.g. channel_name, amount)
 */
public record NotificationRequest(
        long recipientUserId,
        @NonNull NotificationType type,
        @NonNull Map<String, String> variables
) {

    /** Creates a request with null-checks and a defensive copy of variables. */
    public NotificationRequest {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(variables, "variables");
        variables = Map.copyOf(variables);
    }
}

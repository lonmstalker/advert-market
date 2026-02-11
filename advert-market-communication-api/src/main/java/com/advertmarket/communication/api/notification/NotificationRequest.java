package com.advertmarket.communication.api.notification;

import java.util.Map;

/**
 * Request to send a notification to a user.
 *
 * @param recipientUserId Telegram user id of the recipient
 * @param type            notification type determining the template
 * @param variables       template variables (e.g. channel_name, amount)
 */
public record NotificationRequest(
        long recipientUserId,
        NotificationType type,
        Map<String, String> variables
) {

    /** Creates a notification request with an immutable copy of variables. */
    public NotificationRequest {
        variables = Map.copyOf(variables);
    }
}

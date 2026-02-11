package com.advertmarket.communication.api.notification;

/**
 * Port for sending notifications to users.
 *
 * <p>Other modules (deal, financial) depend on this interface
 * through communication-api. The implementation lives in the
 * communication module.
 */
public interface NotificationPort {

    /**
     * Sends a notification to the specified recipient.
     *
     * @param request the notification to send
     * @return true if the notification was delivered successfully
     */
    boolean send(NotificationRequest request);
}

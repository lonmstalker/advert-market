package com.advertmarket.communication.api.event;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Inline keyboard button for a notification message.
 *
 * @param text button label
 * @param url button target URL
 */
public record NotificationButton(
        @NonNull String text,
        @NonNull String url) {
}

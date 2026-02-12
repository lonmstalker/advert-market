package com.advertmarket.delivery.api.event;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Inline keyboard button for a Telegram post.
 *
 * @param text button label
 * @param url button target URL
 */
public record InlineButton(
        @NonNull String text,
        @NonNull String url) {
}

package com.advertmarket.delivery.api.event;

import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Creative content to be published.
 *
 * @param text the post text content
 * @param media list of media URLs (images, videos)
 * @param buttons inline buttons attached to the post
 */
public record CreativeDraft(
        @NonNull String text,
        @NonNull List<String> media,
        @NonNull List<InlineButton> buttons) {

    /**
     * Creates a creative draft with defensive copies.
     *
     * @throws NullPointerException if any parameter is null
     */
    public CreativeDraft {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(media, "media");
        Objects.requireNonNull(buttons, "buttons");
        media = List.copyOf(media);
        buttons = List.copyOf(buttons);
    }
}

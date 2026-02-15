package com.advertmarket.shared.pagination;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A page of results with an optional cursor for the next page.
 *
 * <p>The {@code nextCursor} must be {@code null} when there are
 * no more pages. An empty string is not treated as absent.
 *
 * @param <T> the item type
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CursorPage<T>(
        @NonNull List<T> items,
        @Nullable String nextCursor) {

    /**
     * Creates a cursor page with a defensive copy of items.
     *
     * @throws NullPointerException if items is null
     */
    public CursorPage {
        Objects.requireNonNull(items, "items");
        items = List.copyOf(items);
    }

    /** Returns an empty page with no next cursor. */
    public static <T> @NonNull CursorPage<T> empty() {
        return new CursorPage<>(List.of(), null);
    }

    /** Returns {@code true} if there are more pages. */
    public boolean hasMore() {
        return nextCursor != null;
    }

    /**
     * Jackson-visible alias for {@link #hasMore()}.
     * Frontend pagination schema expects this field.
     */
    @JsonProperty("hasNext")
    public boolean hasNext() {
        return hasMore();
    }
}

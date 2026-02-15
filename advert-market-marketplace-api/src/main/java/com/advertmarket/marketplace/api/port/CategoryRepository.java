package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.dto.CategoryDto;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Repository port for category data.
 */
public interface CategoryRepository {

    /** Returns all active categories ordered by sort_order. */
    @NonNull
    List<CategoryDto> findAllActive();

    /** Returns category slugs for a given channel. */
    @NonNull
    List<String> findCategorySlugsForChannel(long channelId);

    /** Returns category slugs grouped by channel ID for the given channels. */
    @NonNull
    Map<Long, List<String>> findCategorySlugsForChannels(
            @NonNull List<Long> channelIds);
}

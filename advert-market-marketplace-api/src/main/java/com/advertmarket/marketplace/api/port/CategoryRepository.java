package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.dto.CategoryDto;
import java.util.List;
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
}

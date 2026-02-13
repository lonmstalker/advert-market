package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.dto.ChannelListItem;
import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import com.advertmarket.shared.pagination.CursorPage;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for channel search.
 *
 * <p>Current implementation uses ParadeDB pg_search BM25 index.
 * Future implementations may use Meilisearch or Elasticsearch.
 */
public interface ChannelSearchPort {

    /**
     * Searches active channels by the given criteria.
     *
     * @param criteria search filters, sort, and pagination
     * @return page of matching channels
     */
    @NonNull
    CursorPage<ChannelListItem> search(@NonNull ChannelSearchCriteria criteria);

    /**
     * Counts channels matching the given criteria.
     *
     * @param criteria search filters
     * @return total matching rows
     */
    long count(@NonNull ChannelSearchCriteria criteria);
}

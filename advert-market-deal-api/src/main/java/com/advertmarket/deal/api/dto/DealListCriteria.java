package com.advertmarket.deal.api.dto;

import com.advertmarket.shared.model.DealStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Criteria for listing deals with cursor-based pagination.
 *
 * @param status optional status filter
 * @param cursor opaque cursor for keyset pagination
 * @param limit page size (default 20, max 100)
 */
@Schema(description = "Deal list filter criteria")
public record DealListCriteria(
        @Nullable DealStatus status,
        @Nullable String cursor,
        int limit) {

    /** Default page size. */
    public static final int DEFAULT_LIMIT = 20;

    /** Maximum page size. */
    public static final int MAX_LIMIT = 100;

    /** Clamps limit to [1, {@value MAX_LIMIT}]. */
    public DealListCriteria {
        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        }
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
    }
}

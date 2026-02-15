package com.advertmarket.deal.web;

import com.advertmarket.shared.model.DealStatus;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Query params for {@code GET /api/v1/deals}.
 */
record DealListRequestParams(
        @Nullable DealStatus status,
        @Nullable String cursor,
        @Nullable Integer limit
) {

    DealListRequestParams {
        if (limit == null) {
            limit = 20;
        }
    }

    int limitOrDefault() {
        return limit != null ? limit : 20;
    }
}


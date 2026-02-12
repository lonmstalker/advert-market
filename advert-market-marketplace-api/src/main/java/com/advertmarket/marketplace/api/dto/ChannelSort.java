package com.advertmarket.marketplace.api.dto;

/**
 * Sort options for channel search.
 */
public enum ChannelSort {

    RELEVANCE("score", true),
    SUBSCRIBERS_DESC("subscriber_count", true),
    SUBSCRIBERS_ASC("subscriber_count", false),
    PRICE_ASC("price_per_post_nano", false),
    PRICE_DESC("price_per_post_nano", true),
    ENGAGEMENT_DESC("engagement_rate", true),
    UPDATED("updated_at", true);

    private final String fieldName;
    private final boolean descending;

    ChannelSort(String fieldName, boolean descending) {
        this.fieldName = fieldName;
        this.descending = descending;
    }

    public String fieldName() {
        return fieldName;
    }

    public boolean isDescending() {
        return descending;
    }
}

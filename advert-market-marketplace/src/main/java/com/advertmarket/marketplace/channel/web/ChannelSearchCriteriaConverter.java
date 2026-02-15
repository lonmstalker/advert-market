package com.advertmarket.marketplace.channel.web;

import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import com.advertmarket.marketplace.api.dto.ChannelSort;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import java.util.Locale;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

/**
 * Converts REST query params into {@link ChannelSearchCriteria}.
 *
 * <p>Centralizes legacy aliases and validation for controller endpoints.
 */
@Component
public class ChannelSearchCriteriaConverter {

    /**
     * Converts controller query params into {@link ChannelSearchCriteria}.
     */
    public ChannelSearchCriteria fromRequestParams(
            ChannelSearchRequestParams params) {
        return fromRequestParams(
                params.query(),
                params.q(),
                params.category(),
                params.minSubscribers(),
                params.minSubs(),
                params.maxSubscribers(),
                params.maxSubs(),
                params.minPrice(),
                params.maxPrice(),
                params.minEngagement(),
                params.language(),
                params.sort(),
                params.cursor(),
                params.limitOrDefault());
    }

    /**
     * Converts controller query params into {@link ChannelSearchCriteria}.
     */
    public ChannelSearchCriteria fromRequestParams(
            ChannelCountRequestParams params) {
        return fromRequestParams(
                params.query(),
                params.q(),
                params.category(),
                params.minSubscribers(),
                params.minSubs(),
                params.maxSubscribers(),
                params.maxSubs(),
                params.minPrice(),
                params.maxPrice(),
                params.minEngagement(),
                params.language(),
                null,
                null,
                ChannelSearchCriteria.DEFAULT_LIMIT);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private ChannelSearchCriteria fromRequestParams(
            @Nullable String query,
            @Nullable String q,
            @Nullable String category,
            @Nullable Integer minSubscribers,
            @Nullable Integer minSubs,
            @Nullable Integer maxSubscribers,
            @Nullable Integer maxSubs,
            @Nullable Long minPrice,
            @Nullable Long maxPrice,
            @Nullable Double minEngagement,
            @Nullable String language,
            @Nullable String sort,
            @Nullable String cursor,
            int limit) {
        return new ChannelSearchCriteria(
                category,
                firstNonNull(minSubscribers, minSubs),
                firstNonNull(maxSubscribers, maxSubs),
                minPrice,
                maxPrice,
                minEngagement,
                language,
                firstNonBlank(query, q),
                parseSort(sort),
                cursor,
                limit);
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

    private static <T> T firstNonNull(T primary, T fallback) {
        return primary != null ? primary : fallback;
    }

    private static ChannelSort parseSort(String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return ChannelSort.SUBSCRIBERS_DESC;
        }
        String normalized = rawSort.trim().toLowerCase(Locale.ROOT);
        // CHECKSTYLE.SUPPRESS: MissingNullCaseInSwitch for +1 lines
        return switch (normalized) {
            case "relevance" -> ChannelSort.RELEVANCE;
            case "subscribers", "subscribers_desc" -> ChannelSort.SUBSCRIBERS_DESC;
            case "subscribers_asc" -> ChannelSort.SUBSCRIBERS_ASC;
            case "price_asc" -> ChannelSort.PRICE_ASC;
            case "price_desc" -> ChannelSort.PRICE_DESC;
            case "er", "engagement_desc" -> ChannelSort.ENGAGEMENT_DESC;
            case "updated" -> ChannelSort.UPDATED;
            default -> {
                try {
                    yield ChannelSort.valueOf(rawSort.trim().toUpperCase(
                            Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    throw new DomainException(
                            ErrorCodes.INVALID_PARAMETER,
                            "Unsupported sort value: " + rawSort,
                            ex);
                }
            }
        };
    }
}

package com.advertmarket.deal.api.port;

import com.advertmarket.deal.api.dto.DealListCriteria;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Repository port for deal persistence.
 */
public interface DealRepository {

    /**
     * Inserts a new deal record.
     *
     * @param record deal data to persist
     */
    void insert(@NonNull DealRecord record);

    /**
     * Finds a deal by its identifier.
     *
     * @param dealId deal identifier
     * @return deal record, or empty if not found
     */
    @NonNull
    Optional<DealRecord> findById(@NonNull DealId dealId);

    /**
     * Atomically updates the deal status using compare-and-swap.
     *
     * @param dealId deal identifier
     * @param expectedFrom expected current status
     * @param to target status
     * @param expectedVersion expected current version
     * @return number of rows updated (0 = conflict, 1 = success)
     */
    int updateStatus(@NonNull DealId dealId,
                     @NonNull DealStatus expectedFrom,
                     @NonNull DealStatus to,
                     int expectedVersion);

    /**
     * Sets the cancellation reason on a deal.
     *
     * @param dealId deal identifier
     * @param reason cancellation reason text
     */
    void setCancellationReason(@NonNull DealId dealId, @NonNull String reason);

    /**
     * Lists deals for a user (as advertiser or owner) with cursor-based pagination.
     *
     * @param userId user ID (advertiser_id or owner_id)
     * @param criteria filter and pagination parameters
     * @return list of matching deals (size = limit + 1 if more pages exist)
     */
    @NonNull
    List<DealRecord> listByUser(long userId, @NonNull DealListCriteria criteria);
}

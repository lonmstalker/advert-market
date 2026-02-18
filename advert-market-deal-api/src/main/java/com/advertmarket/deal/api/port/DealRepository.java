package com.advertmarket.deal.api.port;

import com.advertmarket.deal.api.dto.DealListCriteria;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import java.time.Duration;
import java.time.Instant;
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

    /**
     * Reassigns deal owner for non-terminal deals.
     *
     * @param dealId deal identifier
     * @param newOwnerId new owner user ID
     * @return true if owner was changed
     */
    boolean reassignOwnerIfNonTerminal(@NonNull DealId dealId, long newOwnerId);

    /**
     * Finds deals whose deadline has expired, applying a grace period.
     * Uses {@code FOR UPDATE SKIP LOCKED} to prevent concurrent processing.
     *
     * @param batchSize maximum number of deals to return
     * @param gracePeriod grace window after deadline to avoid races
     * @return list of expired deals locked for processing
     */
    @NonNull
    List<DealRecord> findExpiredDeals(int batchSize, @NonNull Duration gracePeriod);

    /**
     * Sets the deadline timestamp on a deal.
     *
     * @param dealId deal identifier
     * @param deadlineAt new deadline instant
     */
    void setDeadline(@NonNull DealId dealId, @NonNull Instant deadlineAt);

    /**
     * Clears the deadline on a deal (sets to null).
     *
     * @param dealId deal identifier
     */
    void clearDeadline(@NonNull DealId dealId);

    /**
     * Persists deposit address and subwallet for a deal.
     *
     * @param dealId deal identifier
     * @param depositAddress generated deposit address
     * @param subwalletId subwallet identifier
     */
    void setDepositAddress(
            @NonNull DealId dealId,
            @NonNull String depositAddress,
            int subwalletId);

    /**
     * Marks deal as funded and stores confirmed deposit tx hash.
     *
     * @param dealId deal identifier
     * @param fundedAt funding timestamp
     * @param depositTxHash confirmed deposit transaction hash
     */
    void setFunded(
            @NonNull DealId dealId,
            @NonNull Instant fundedAt,
            @NonNull String depositTxHash);

    /**
     * Stores payout transaction hash.
     *
     * @param dealId deal identifier
     * @param payoutTxHash payout transaction hash
     */
    void setPayoutTxHash(
            @NonNull DealId dealId,
            @NonNull String payoutTxHash);

    /**
     * Stores refund transaction hash.
     *
     * @param dealId deal identifier
     * @param refundedTxHash refund transaction hash
     */
    void setRefundedTxHash(
            @NonNull DealId dealId,
            @NonNull String refundedTxHash);

    /**
     * Persists publication metadata from delivery worker.
     *
     * @param dealId deal identifier
     * @param messageId published Telegram message id
     * @param contentHash published content hash
     * @param publishedAt publication timestamp
     */
    void setPublicationMetadata(
            @NonNull DealId dealId,
            long messageId,
            @NonNull String contentHash,
            @NonNull Instant publishedAt);

    /**
     * Returns all operator user IDs used for dispute escalation alerts.
     *
     * @return ordered list of operator user identifiers
     */
    @NonNull
    List<Long> findOperatorUserIds();
}

package com.advertmarket.deal.api.port;

import com.advertmarket.deal.api.dto.DealEventRecord;
import com.advertmarket.shared.model.DealId;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Repository port for deal event persistence (append-only).
 */
public interface DealEventRepository {

    /**
     * Appends an event to the deal event log.
     *
     * @param record event data to persist
     */
    void append(@NonNull DealEventRecord record);

    /**
     * Finds all events for a deal ordered by creation time descending.
     *
     * @param dealId deal identifier
     * @return list of deal events, newest first
     */
    @NonNull
    List<DealEventRecord> findByDealId(@NonNull DealId dealId);
}

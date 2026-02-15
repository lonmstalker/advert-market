package com.advertmarket.deal.api.port;

import com.advertmarket.deal.api.dto.CreateDealCommand;
import com.advertmarket.deal.api.dto.DealDetailDto;
import com.advertmarket.deal.api.dto.DealDto;
import com.advertmarket.deal.api.dto.DealListCriteria;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.pagination.CursorPage;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Primary port for deal operations.
 */
public interface DealPort {

    /**
     * Creates a new deal in DRAFT status.
     *
     * @param command creation parameters
     * @return the created deal summary
     */
    @NonNull
    DealDto create(@NonNull CreateDealCommand command);

    /**
     * Returns full deal detail including event timeline.
     * Enforces ABAC: caller must be a participant.
     *
     * @param dealId deal identifier
     * @return deal detail with timeline
     */
    @NonNull
    DealDetailDto getDetail(@NonNull DealId dealId);

    /**
     * Lists deals for the current user with cursor-based pagination.
     *
     * @param criteria filter and pagination parameters
     * @return paginated deal list
     */
    @NonNull
    CursorPage<DealDto> listForUser(@NonNull DealListCriteria criteria);

    /**
     * Transitions a deal to a new status.
     *
     * @param command transition parameters
     * @return transition result (success or idempotent)
     */
    @NonNull
    DealTransitionResult transition(@NonNull DealTransitionCommand command);
}

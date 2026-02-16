package com.advertmarket.marketplace.channel.service;

import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelListItem;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.port.ChannelAutoSyncPort;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.api.port.ChannelSearchPort;
import com.advertmarket.marketplace.channel.mapper.ChannelSearchCriteriaNormalizer;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.pagination.CursorPage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Channel catalog operations: search, detail, update, deactivate.
 */
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelSearchPort searchPort;
    private final ChannelRepository channelRepository;
    private final ChannelAuthorizationPort authorizationPort;
    private final ChannelAutoSyncPort channelAutoSyncPort;

    /**
     * Searches active channels by the given criteria.
     *
     * @param criteria search filters, sort, and pagination
     * @return page of matching channels
     */
    @NonNull
    public CursorPage<ChannelListItem> search(
            @NonNull ChannelSearchCriteria criteria) {
        return searchPort.search(
                ChannelSearchCriteriaNormalizer.normalize(criteria));
    }

    /**
     * Counts active channels by the given criteria.
     *
     * @param criteria search filters
     * @return count of matching channels
     */
    public long count(@NonNull ChannelSearchCriteria criteria) {
        return searchPort.count(
                ChannelSearchCriteriaNormalizer.normalize(criteria));
    }

    /**
     * Returns all active channels owned by the given user.
     *
     * @param userId user ID
     * @return list of owned channels
     */
    @NonNull
    public List<ChannelResponse> findByMemberUserId(long userId) {
        return channelRepository.findByMemberUserId(userId);
    }

    /**
     * Returns full channel detail including pricing rules.
     *
     * @param channelId channel ID
     * @return channel detail
     * @throws DomainException CHANNEL_NOT_FOUND if not found
     */
    @NonNull
    public ChannelDetailResponse getDetail(long channelId) {
        return channelRepository.findDetailById(channelId)
                .orElseThrow(() -> new DomainException(
                        ErrorCodes.CHANNEL_NOT_FOUND,
                        "Channel not found: " + channelId));
    }

    /**
     * Updates channel details. Only the owner can update.
     *
     * @param channelId channel ID
     * @param request   fields to update
     * @return updated channel
     * @throws DomainException CHANNEL_NOT_OWNED if not owner
     * @throws DomainException CHANNEL_NOT_FOUND if not found
     */
    @NonNull
    @Transactional
    public ChannelResponse update(long channelId,
                                  @NonNull ChannelUpdateRequest request) {
        requireOwner(channelId);
        channelAutoSyncPort.syncFromTelegram(channelId);
        return channelRepository.update(channelId, request)
                .orElseThrow(() -> new DomainException(
                        ErrorCodes.CHANNEL_NOT_FOUND,
                        "Channel not found: " + channelId));
    }

    /**
     * Deactivates a channel. Only the owner can deactivate.
     *
     * @param channelId channel ID
     * @throws DomainException CHANNEL_NOT_OWNED if not owner
     * @throws DomainException CHANNEL_NOT_FOUND if not found
     */
    @Transactional
    public void deactivate(long channelId) {
        requireOwner(channelId);
        channelAutoSyncPort.syncFromTelegram(channelId);
        boolean deactivated = channelRepository.deactivate(channelId);
        if (!deactivated) {
            throw new DomainException(
                    ErrorCodes.CHANNEL_NOT_FOUND,
                    "Channel not found or already inactive: " + channelId);
        }
    }

    private void requireOwner(long channelId) {
        if (!authorizationPort.isOwner(channelId)) {
            throw new DomainException(
                    ErrorCodes.CHANNEL_NOT_OWNED,
                    "Not the owner of channel: " + channelId);
        }
    }

}

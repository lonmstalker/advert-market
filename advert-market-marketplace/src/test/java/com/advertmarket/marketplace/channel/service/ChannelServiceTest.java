package com.advertmarket.marketplace.channel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelListItem;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import com.advertmarket.marketplace.api.dto.ChannelSort;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.api.port.ChannelSearchPort;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.pagination.CursorPage;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ChannelService — search, detail, update, deactivate")
@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    private static final long CHANNEL_ID = -1001234567890L;

    @Mock
    private ChannelSearchPort searchPort;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private ChannelAuthorizationPort authorizationPort;

    @InjectMocks
    private ChannelService channelService;

    @Test
    @DisplayName("Should delegate search to ChannelSearchPort")
    void shouldDelegateSearch() {
        var criteria = new ChannelSearchCriteria(
                null, null, null, null, null, null,
                null, null, ChannelSort.SUBSCRIBERS_DESC, null, 20);
        var expected = new CursorPage<>(List.of(channelListItem()), null);

        when(searchPort.search(any())).thenReturn(expected);

        var result = channelService.search(criteria);

        assertThat(result.items()).hasSize(1);
        verify(searchPort).search(any());
    }

    @Test
    @DisplayName("Should clamp limit to MAX_LIMIT")
    void shouldClampLimit() {
        var criteria = new ChannelSearchCriteria(
                null, null, null, null, null, null,
                null, null, ChannelSort.SUBSCRIBERS_DESC, null, 100);
        when(searchPort.search(any()))
                .thenReturn(CursorPage.empty());

        channelService.search(criteria);

        var captor = ArgumentCaptor.forClass(ChannelSearchCriteria.class);
        verify(searchPort).search(captor.capture());
        assertThat(captor.getValue().limit()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should delegate count to ChannelSearchPort")
    void shouldDelegateCount() {
        var criteria = new ChannelSearchCriteria(
                null, null, null, null, null, null,
                null, "crypto", ChannelSort.SUBSCRIBERS_DESC, null, 20);
        when(searchPort.count(any())).thenReturn(42L);

        long count = channelService.count(criteria);

        assertThat(count).isEqualTo(42L);
        verify(searchPort).count(criteria);
    }

    @Test
    @DisplayName("Should return channel detail when found")
    void shouldReturnDetail() {
        when(channelRepository.findDetailById(CHANNEL_ID))
                .thenReturn(Optional.of(channelDetail()));

        var result = channelService.getDetail(CHANNEL_ID);

        assertThat(result.id()).isEqualTo(CHANNEL_ID);
        assertThat(result.title()).isEqualTo("Test Channel");
    }

    @Test
    @DisplayName("Should throw CHANNEL_NOT_FOUND when detail not found")
    void shouldThrowWhenDetailNotFound() {
        when(channelRepository.findDetailById(CHANNEL_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.getDetail(CHANNEL_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CHANNEL_NOT_FOUND);
    }

    @Test
    @DisplayName("Should update channel when owner")
    void shouldUpdateWhenOwner() {
        when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
        var request = new ChannelUpdateRequest(
                "new desc", null, null, null, null);
        when(channelRepository.update(eq(CHANNEL_ID), any()))
                .thenReturn(Optional.of(channelResponse()));

        var result = channelService.update(CHANNEL_ID, request);

        assertThat(result.id()).isEqualTo(CHANNEL_ID);
    }

    @Test
    @DisplayName("Should throw CHANNEL_NOT_OWNED on update when not owner")
    void shouldThrowWhenNotOwnerUpdate() {
        when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(false);
        var request = new ChannelUpdateRequest(
                null, null, null, null, null);

        assertThatThrownBy(
                () -> channelService.update(CHANNEL_ID, request))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CHANNEL_NOT_OWNED);
    }

    @Test
    @DisplayName("Should deactivate channel when owner")
    void shouldDeactivateWhenOwner() {
        when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
        when(channelRepository.deactivate(CHANNEL_ID)).thenReturn(true);

        channelService.deactivate(CHANNEL_ID);

        verify(channelRepository).deactivate(CHANNEL_ID);
    }

    @Test
    @DisplayName("Should throw CHANNEL_NOT_OWNED on deactivate when not owner")
    void shouldThrowWhenNotOwnerDeactivate() {
        when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(false);

        assertThatThrownBy(() -> channelService.deactivate(CHANNEL_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CHANNEL_NOT_OWNED);
    }

    @Test
    @DisplayName("Should delegate findByOwnerId to ChannelRepository")
    void shouldDelegateFindByOwnerId() {
        var expected = List.of(channelResponse());
        when(channelRepository.findByOwnerId(222L)).thenReturn(expected);

        var result = channelService.findByOwnerId(222L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(CHANNEL_ID);
        verify(channelRepository).findByOwnerId(222L);
    }

    private static ChannelListItem channelListItem() {
        return new ChannelListItem(
                CHANNEL_ID, "Test Channel", "test",
                List.of("tech"), 5000, 1000,
                BigDecimal.valueOf(3.5), 100_000_000L,
                true, OffsetDateTime.now());
    }

    private static ChannelDetailResponse channelDetail() {
        return new ChannelDetailResponse(
                CHANNEL_ID, "Test Channel", "test",
                "Description", 5000, List.of("tech"),
                100_000_000L, true, 222L,
                BigDecimal.valueOf(3.5), 1000, "ru",
                List.of(), OffsetDateTime.now(), OffsetDateTime.now());
    }

    private static ChannelResponse channelResponse() {
        return new ChannelResponse(
                CHANNEL_ID, "Test Channel", "test",
                null, 5000, List.of("tech"),
                null, true, 222L,
                OffsetDateTime.now());
    }
}

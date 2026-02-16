package com.advertmarket.marketplace.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.marketplace.api.dto.PricingRuleCreateRequest;
import com.advertmarket.marketplace.api.dto.PricingRuleDto;
import com.advertmarket.marketplace.api.dto.PricingRuleUpdateRequest;
import com.advertmarket.marketplace.api.model.ChannelRight;
import com.advertmarket.marketplace.api.model.PostType;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.port.ChannelAutoSyncPort;
import com.advertmarket.marketplace.api.port.PricingRuleRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PricingRuleService — CRUD operations")
@ExtendWith(MockitoExtension.class)
class PricingRuleServiceTest {

    private static final long CHANNEL_ID = -1001234567890L;
    private static final long RULE_ID = 1L;

    @Mock
    private PricingRuleRepository pricingRuleRepository;
    @Mock
    private ChannelAuthorizationPort authorizationPort;
    @Mock
    private ChannelAutoSyncPort channelAutoSyncPort;

    @InjectMocks
    private PricingRuleService pricingRuleService;

    @Test
    @DisplayName("Should list pricing rules by channel")
    void shouldListByChannel() {
        when(pricingRuleRepository.findByChannelId(CHANNEL_ID))
                .thenReturn(List.of(pricingRule()));

        var result = pricingRuleService.listByChannel(CHANNEL_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Repost");
    }

    @Test
    @DisplayName("Should create pricing rule when owner")
    void shouldCreateWhenOwner() {
        when(authorizationPort.hasRight(
                CHANNEL_ID, ChannelRight.MANAGE_LISTINGS))
                .thenReturn(true);
        var request = new PricingRuleCreateRequest(
                "Repost", null, Set.of(PostType.REPOST), 50_000_000L, 0);
        when(pricingRuleRepository.insert(eq(CHANNEL_ID), any()))
                .thenReturn(pricingRule());

        var result = pricingRuleService.create(CHANNEL_ID, request);

        assertThat(result.name()).isEqualTo("Repost");
        verify(channelAutoSyncPort).syncFromTelegram(CHANNEL_ID);
    }

    @Test
    @DisplayName("Should throw CHANNEL_NOT_OWNED on create when not owner")
    void shouldThrowOnCreateWhenNotOwner() {
        when(authorizationPort.hasRight(
                CHANNEL_ID, ChannelRight.MANAGE_LISTINGS))
                .thenReturn(false);
        var request = new PricingRuleCreateRequest(
                "Repost", null, Set.of(PostType.REPOST), 50_000_000L, 0);

        assertThatThrownBy(
                () -> pricingRuleService.create(CHANNEL_ID, request))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CHANNEL_NOT_OWNED);
        verify(channelAutoSyncPort, never()).syncFromTelegram(CHANNEL_ID);
    }

    @Test
    @DisplayName("Should update pricing rule when owner")
    void shouldUpdateWhenOwner() {
        when(authorizationPort.hasRight(
                CHANNEL_ID, ChannelRight.MANAGE_LISTINGS))
                .thenReturn(true);
        var request = new PricingRuleUpdateRequest(
                "Updated", null, null, null, null, null);
        when(pricingRuleRepository.update(eq(RULE_ID), any()))
                .thenReturn(Optional.of(pricingRule()));

        var result = pricingRuleService.update(
                CHANNEL_ID, RULE_ID, request);

        assertThat(result.name()).isEqualTo("Repost");
        verify(channelAutoSyncPort).syncFromTelegram(CHANNEL_ID);
    }

    @Test
    @DisplayName("Should throw PRICING_RULE_NOT_FOUND on update when missing")
    void shouldThrowOnUpdateWhenMissing() {
        when(authorizationPort.hasRight(
                CHANNEL_ID, ChannelRight.MANAGE_LISTINGS))
                .thenReturn(true);
        var request = new PricingRuleUpdateRequest(
                "Updated", null, null, null, null, null);
        when(pricingRuleRepository.update(eq(RULE_ID), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pricingRuleService.update(
                CHANNEL_ID, RULE_ID, request))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.PRICING_RULE_NOT_FOUND);
        verify(channelAutoSyncPort).syncFromTelegram(CHANNEL_ID);
    }

    @Test
    @DisplayName("Should soft-delete pricing rule when owner")
    void shouldDeleteWhenOwner() {
        when(authorizationPort.hasRight(
                CHANNEL_ID, ChannelRight.MANAGE_LISTINGS))
                .thenReturn(true);
        when(pricingRuleRepository.deactivate(RULE_ID)).thenReturn(true);

        pricingRuleService.delete(CHANNEL_ID, RULE_ID);

        verify(channelAutoSyncPort).syncFromTelegram(CHANNEL_ID);
        verify(pricingRuleRepository).deactivate(RULE_ID);
    }

    @Test
    @DisplayName("Should throw PRICING_RULE_NOT_FOUND on delete when missing")
    void shouldThrowOnDeleteWhenMissing() {
        when(authorizationPort.hasRight(
                CHANNEL_ID, ChannelRight.MANAGE_LISTINGS))
                .thenReturn(true);
        when(pricingRuleRepository.deactivate(RULE_ID)).thenReturn(false);

        assertThatThrownBy(
                () -> pricingRuleService.delete(CHANNEL_ID, RULE_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.PRICING_RULE_NOT_FOUND);
        verify(channelAutoSyncPort).syncFromTelegram(CHANNEL_ID);
    }

    private static PricingRuleDto pricingRule() {
        return new PricingRuleDto(
                RULE_ID, CHANNEL_ID, "Repost", null,
                Set.of(PostType.REPOST), 50_000_000L, true, 0);
    }
}

import { describe, expect, it } from 'vitest';
import {
  deriveDealRole,
  mapDealDetailDtoToViewModel,
  mapDealDtoToViewModel,
  mapDealEventDtoToViewModel,
} from '../deal-mapper';

const baseDealDto = {
  id: 'deal-1',
  channelId: 101,
  advertiserId: 10,
  ownerId: 20,
  status: 'OFFER_PENDING' as const,
  amountNano: 3_000_000_000,
  deadlineAt: null,
  createdAt: '2026-01-01T00:00:00Z',
  version: 1,
};

describe('deal-mapper', () => {
  it('derives advertiser role by profile id', () => {
    expect(deriveDealRole(baseDealDto, 10)).toBe('ADVERTISER');
  });

  it('derives owner role by profile id', () => {
    expect(deriveDealRole(baseDealDto, 20)).toBe('OWNER');
  });

  it('returns null role when user is not a participant', () => {
    expect(deriveDealRole(baseDealDto, 99)).toBeNull();
  });

  it('maps deal dto to vm with fallback channel metadata', () => {
    const vm = mapDealDtoToViewModel(baseDealDto, {
      profileId: 10,
      channel: null,
    });

    expect(vm.id).toBe('deal-1');
    expect(vm.role).toBe('ADVERTISER');
    expect(vm.priceNano).toBe(baseDealDto.amountNano);
    expect(vm.channelTitle).toBe('Channel #101');
    expect(vm.channelUsername).toBeNull();
    expect(vm.postType).toBe('NATIVE');
  });

  it('uses enrichment channel metadata when available', () => {
    const vm = mapDealDtoToViewModel(baseDealDto, {
      profileId: 20,
      channel: {
        title: 'Tech Digest',
        username: 'techdigest',
        postType: 'REPOST',
        durationHours: 24,
        postFrequencyHours: 12,
      },
    });

    expect(vm.role).toBe('OWNER');
    expect(vm.channelTitle).toBe('Tech Digest');
    expect(vm.channelUsername).toBe('techdigest');
    expect(vm.postType).toBe('REPOST');
    expect(vm.durationHours).toBe(24);
    expect(vm.postFrequencyHours).toBe(12);
  });

  it('maps event dto to vm status using toStatus first', () => {
    const event = mapDealEventDtoToViewModel({
      id: 1,
      eventType: 'DEAL_STATE_CHANGED',
      fromStatus: 'DRAFT',
      toStatus: 'OFFER_PENDING',
      actorId: 10,
      createdAt: '2026-01-01T00:00:00Z',
    });

    expect(event.status).toBe('OFFER_PENDING');
    expect(event.type).toBe('DEAL_STATE_CHANGED');
  });

  it('maps detail dto timeline to vm timeline', () => {
    const detailVm = mapDealDetailDtoToViewModel(
      {
        ...baseDealDto,
        commissionRateBp: 200,
        commissionNano: 60_000_000,
        timeline: [
          {
            id: 1,
            eventType: 'DEAL_STATE_CHANGED',
            fromStatus: 'DRAFT',
            toStatus: 'OFFER_PENDING',
            actorId: 10,
            createdAt: '2026-01-01T00:00:00Z',
          },
        ],
      },
      {
        profileId: 10,
        channel: null,
      },
    );

    expect(detailVm.timeline).toHaveLength(1);
    expect(detailVm.timeline[0]?.status).toBe('OFFER_PENDING');
    expect(detailVm.commissionNano).toBe(60_000_000);
  });
});

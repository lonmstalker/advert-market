import { describe, expect, it, vi } from 'vitest';
import { api } from '@/shared/api/client';
import { createChannelPricingRule, createDeal, fetchPostTypes, updateChannel } from './channels';

vi.mock('@/shared/api/client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('channels api', () => {
  it('createDeal sends backend payload amountNano + creativeBrief and returns DealDto', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({
      id: 'deal-1',
      channelId: 1,
      advertiserId: 10,
      ownerId: 20,
      status: 'DRAFT',
      amountNano: 3_000_000_000,
      deadlineAt: null,
      createdAt: '2026-01-01T00:00:00Z',
      version: 1,
    });

    const result = await createDeal({
      channelId: 1,
      amountNano: 3_000_000_000,
      creativeBrief: 'Need native integration',
    });

    expect(api.post).toHaveBeenCalledWith(
      '/deals',
      {
        channelId: 1,
        amountNano: 3_000_000_000,
        creativeBrief: 'Need native integration',
      },
      expect.any(Object),
    );

    expect(result).toEqual({
      id: 'deal-1',
      channelId: 1,
      advertiserId: 10,
      ownerId: 20,
      status: 'DRAFT',
      amountNano: 3_000_000_000,
      deadlineAt: null,
      createdAt: '2026-01-01T00:00:00Z',
      version: 1,
    });
  });

  it('createDeal sends optional creativeId when creating from saved creative', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({
      id: 'deal-2',
      channelId: 5,
      advertiserId: 10,
      ownerId: 20,
      status: 'DRAFT',
      amountNano: 4_000_000_000,
      deadlineAt: null,
      createdAt: '2026-01-01T00:00:00Z',
      version: 1,
    });

    await createDeal({
      channelId: 5,
      amountNano: 4_000_000_000,
      pricingRuleId: 10,
      creativeId: 'creative-42',
    });

    expect(api.post).toHaveBeenCalledWith(
      '/deals',
      {
        channelId: 5,
        amountNano: 4_000_000_000,
        pricingRuleId: 10,
        creativeId: 'creative-42',
      },
      expect.any(Object),
    );
  });

  it('fetchPostTypes loads localized post types from /post-types', async () => {
    vi.mocked(api.get).mockResolvedValueOnce([
      {
        type: 'NATIVE',
        labels: {
          en: 'Native ad',
          ru: 'Нативная реклама',
        },
      },
    ]);

    const result = await fetchPostTypes();

    expect(api.get).toHaveBeenCalledWith('/post-types', expect.any(Object));
    expect(result).toEqual([
      {
        type: 'NATIVE',
        labels: {
          en: 'Native ad',
          ru: 'Нативная реклама',
        },
      },
    ]);
  });

  it('createChannelPricingRule sends POST to channel pricing endpoint', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({
      id: 15,
      channelId: 7,
      name: 'Story',
      description: null,
      postTypes: ['STORY'],
      priceNano: 3_000_000_000,
      isActive: true,
      sortOrder: 0,
    });

    await createChannelPricingRule(7, {
      name: 'Story',
      postTypes: ['STORY'],
      priceNano: 3_000_000_000,
      sortOrder: 0,
    });

    expect(api.post).toHaveBeenCalledWith(
      '/channels/7/pricing',
      {
        name: 'Story',
        postTypes: ['STORY'],
        priceNano: 3_000_000_000,
        sortOrder: 0,
      },
      expect.any(Object),
    );
  });

  it('updateChannel sends owner note and categories to PUT /channels/{id}', async () => {
    vi.mocked(api.put).mockResolvedValueOnce({
      id: 77,
      title: 'Channel 77',
      subscriberCount: 100,
      categories: ['tech'],
      pricePerPostNano: 2_000_000_000,
      isActive: true,
      ownerId: 1,
      createdAt: '2026-01-01T00:00:00Z',
    });

    await updateChannel(77, {
      categories: ['tech'],
      customRules: 'No casino ads',
    });

    expect(api.put).toHaveBeenCalledWith(
      '/channels/77',
      {
        categories: ['tech'],
        customRules: 'No casino ads',
      },
      expect.any(Object),
    );
  });
});

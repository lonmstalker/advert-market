import { describe, expect, it, vi } from 'vitest';
import { api } from '@/shared/api/client';
import { createDeal } from './channels';

vi.mock('@/shared/api/client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
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
});

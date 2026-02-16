import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/shared/api';
import { api } from '@/shared/api/client';
import { fetchDeal, fetchDealDeposit, fetchDeals, transitionDeal } from './deals';

vi.mock('@/shared/api/client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

function buildApiError(status: number, title: string) {
  return new ApiError(status, {
    type: 'about:blank',
    title,
    status,
  });
}

describe('deals api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetchDeals returns empty page when endpoint is unavailable', async () => {
    vi.mocked(api.get).mockRejectedValueOnce(buildApiError(404, 'Not Found'));

    const page = await fetchDeals({ cursor: undefined });

    expect(page).toEqual({
      items: [],
      nextCursor: null,
      hasNext: false,
    });
  });

  it('fetchDeals sends backend-supported query params only', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      items: [],
      nextCursor: null,
      hasNext: false,
    });

    await fetchDeals({ status: 'OFFER_PENDING', cursor: 'abc', limit: 30 });

    expect(api.get).toHaveBeenCalledWith(
      '/deals',
      expect.objectContaining({
        params: {
          status: 'OFFER_PENDING',
          cursor: 'abc',
          limit: 30,
        },
      }),
    );
  });

  it('fetchDeal uses /deals/{id} detail endpoint', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      id: 'deal-1',
      channelId: 1,
      advertiserId: 1,
      ownerId: 2,
      status: 'OFFER_PENDING',
      amountNano: 1_000_000_000,
      commissionRateBp: 200,
      commissionNano: 20_000_000,
      deadlineAt: null,
      createdAt: '2026-01-01T00:00:00Z',
      version: 1,
      timeline: [],
    });

    await fetchDeal('deal-1');

    expect(api.get).toHaveBeenCalledWith('/deals/deal-1', expect.any(Object));
  });

  it('fetchDealDeposit uses /deals/{id}/deposit endpoint', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      escrowAddress: 'UQ_TEST',
      amountNano: '1000000000',
      dealId: 'deal-1',
      status: 'AWAITING_PAYMENT',
      currentConfirmations: null,
      requiredConfirmations: null,
      receivedAmountNano: null,
      txHash: null,
      expiresAt: '2026-01-01T01:00:00Z',
    });

    await fetchDealDeposit('deal-1');

    expect(api.get).toHaveBeenCalledWith('/deals/deal-1/deposit', expect.any(Object));
  });

  it('transitionDeal sends targetStatus payload and parses transition response', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({
      status: 'SUCCESS',
      newStatus: 'ACCEPTED',
      currentStatus: null,
    });

    const result = await transitionDeal('deal-1', {
      targetStatus: 'ACCEPTED',
      reason: 'Terms accepted',
    });

    expect(api.post).toHaveBeenCalledWith(
      '/deals/deal-1/transition',
      {
        targetStatus: 'ACCEPTED',
        reason: 'Terms accepted',
      },
      expect.any(Object),
    );
    expect(result).toEqual({
      status: 'SUCCESS',
      newStatus: 'ACCEPTED',
      currentStatus: null,
    });
  });

  it('rethrows non-availability errors from fetchDeals', async () => {
    vi.mocked(api.get).mockRejectedValueOnce(buildApiError(500, 'Internal Server Error'));

    await expect(fetchDeals({})).rejects.toBeInstanceOf(ApiError);
  });
});

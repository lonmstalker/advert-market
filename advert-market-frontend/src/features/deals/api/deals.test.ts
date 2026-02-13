import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/shared/api';
import { api } from '@/shared/api/client';
import { fetchDeals } from './deals';

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

describe('fetchDeals', () => {
  it('returns empty page when deals API endpoint is unavailable', async () => {
    vi.mocked(api.get).mockRejectedValueOnce(buildApiError(404, 'Not Found'));

    const page = await fetchDeals({ role: 'ADVERTISER' });

    expect(page).toEqual({
      items: [],
      nextCursor: null,
      hasNext: false,
    });
  });

  it('rethrows non-availability errors', async () => {
    vi.mocked(api.get).mockRejectedValueOnce(buildApiError(500, 'Internal Server Error'));

    await expect(fetchDeals({ role: 'OWNER' })).rejects.toBeInstanceOf(ApiError);
  });
});

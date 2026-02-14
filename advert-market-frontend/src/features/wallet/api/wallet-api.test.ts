import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/shared/api/types';

const mockGet = vi.fn();

vi.mock('@/shared/api/client', () => ({
  api: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

vi.mock('@/shared/lib/telegram-init-data', () => ({
  getTelegramInitData: () => null,
}));

import { fetchTransactionDetail, fetchTransactions, fetchWalletSummary } from './wallet-api';

function makeApiError(status: number): ApiError {
  return new ApiError(status, { type: 'about:blank', title: 'Error', status });
}

describe('fetchWalletSummary', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubEnv('VITE_MOCK_API', 'false');
  });

  it('returns summary on success', async () => {
    const summary = {
      earnedTotalNano: '10',
      inEscrowNano: '0',
      spentTotalNano: '0',
      activeEscrowNano: '0',
      activeDealsCount: 1,
      completedDealsCount: 2,
    };
    mockGet.mockResolvedValue(summary);

    const result = await fetchWalletSummary();
    expect(result).toEqual(summary);
    expect(mockGet).toHaveBeenCalledWith('/wallet/summary', expect.any(Object));
  });

  it('returns EMPTY_SUMMARY on 404 (graceful fallback)', async () => {
    mockGet.mockRejectedValue(makeApiError(404));

    const result = await fetchWalletSummary();
    expect(result.earnedTotalNano).toBe('0');
    expect(result.completedDealsCount).toBe(0);
  });

  it('returns EMPTY_SUMMARY on 405', async () => {
    mockGet.mockRejectedValue(makeApiError(405));

    const result = await fetchWalletSummary();
    expect(result.earnedTotalNano).toBe('0');
  });

  it('returns EMPTY_SUMMARY on 501', async () => {
    mockGet.mockRejectedValue(makeApiError(501));

    const result = await fetchWalletSummary();
    expect(result.earnedTotalNano).toBe('0');
  });

  it('rethrows on 500', async () => {
    mockGet.mockRejectedValue(makeApiError(500));

    await expect(fetchWalletSummary()).rejects.toThrow(ApiError);
  });
});

describe('fetchTransactions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubEnv('VITE_MOCK_API', 'false');
  });

  it('returns paginated list on success', async () => {
    const page = { items: [{ id: 'tx-1' }], nextCursor: null, hasNext: false };
    mockGet.mockResolvedValue(page);

    const result = await fetchTransactions({});
    expect(result.items).toHaveLength(1);
  });

  it('passes filter and pagination params', async () => {
    mockGet.mockResolvedValue({ items: [], nextCursor: null, hasNext: false });

    await fetchTransactions({ cursor: 'c1', limit: 10, type: 'payout', from: '2026-01-01', to: '2026-02-01' });

    expect(mockGet).toHaveBeenCalledWith(
      '/wallet/transactions',
      expect.objectContaining({
        params: expect.objectContaining({
          cursor: 'c1',
          limit: 10,
          type: 'payout',
          from: '2026-01-01',
          to: '2026-02-01',
        }),
      }),
    );
  });

  it('returns empty page on 404 (graceful fallback)', async () => {
    mockGet.mockRejectedValue(makeApiError(404));

    const result = await fetchTransactions({});
    expect(result.items).toEqual([]);
    expect(result.hasNext).toBe(false);
  });
});

describe('fetchTransactionDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns detail by txId', async () => {
    const detail = { id: 'tx-1', txHash: 'abc' };
    mockGet.mockResolvedValue(detail);

    const result = await fetchTransactionDetail('tx-1');
    expect(result).toEqual(detail);
    expect(mockGet).toHaveBeenCalledWith('/wallet/transactions/tx-1', expect.any(Object));
  });

  it('propagates errors (no fallback)', async () => {
    mockGet.mockRejectedValue(makeApiError(500));

    await expect(fetchTransactionDetail('tx-1')).rejects.toThrow(ApiError);
  });
});

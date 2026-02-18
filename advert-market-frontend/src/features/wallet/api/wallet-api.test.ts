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
    expect(mockGet).toHaveBeenCalledWith('/wallet/summary');
  });

  it('maps backend summary shape to frontend wallet summary', async () => {
    mockGet.mockResolvedValue({
      pendingBalanceNano: 2_500_000_000,
      availableBalanceNano: 7_500_000_000,
      totalEarnedNano: 10_000_000_000,
    });

    const result = await fetchWalletSummary();

    expect(result).toEqual({
      earnedTotalNano: '10000000000',
      inEscrowNano: '2500000000',
      spentTotalNano: '0',
      activeEscrowNano: '0',
      activeDealsCount: 0,
      completedDealsCount: 0,
    });
  });

  it('maps backend advertiser summary shape to advertiser wallet view', async () => {
    mockGet.mockResolvedValue({
      pendingBalanceNano: 2_500_000_000,
      availableBalanceNano: 7_500_000_000,
      totalEarnedNano: 0,
    });

    const result = await fetchWalletSummary();

    expect(result).toEqual({
      earnedTotalNano: '0',
      inEscrowNano: '0',
      spentTotalNano: '7500000000',
      activeEscrowNano: '2500000000',
      activeDealsCount: 0,
      completedDealsCount: 0,
    });
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

  it('maps backend ledger entries page to frontend transaction list', async () => {
    mockGet.mockResolvedValue({
      items: [
        {
          id: 101,
          dealId: '00000000-0000-0000-0000-000000000101',
          entryType: 'OWNER_PAYOUT',
          debitNano: 0,
          creditNano: 2_000_000_000,
          description: 'Escrow released',
          createdAt: '2026-02-18T10:00:00Z',
        },
        {
          id: 102,
          dealId: null,
          entryType: 'OWNER_WITHDRAWAL',
          debitNano: 500_000_000,
          creditNano: 0,
          description: 'Withdrawal',
          createdAt: '2026-02-18T11:00:00Z',
        },
      ],
      nextCursor: 'next',
      hasNext: true,
    });

    const result = await fetchTransactions({});

    expect(result.hasNext).toBe(true);
    expect(result.nextCursor).toBe('next');
    expect(result.items).toEqual([
      {
        id: '101',
        type: 'payout',
        status: 'confirmed',
        amountNano: '2000000000',
        direction: 'income',
        dealId: '00000000-0000-0000-0000-000000000101',
        channelTitle: null,
        description: 'Escrow released',
        createdAt: '2026-02-18T10:00:00Z',
      },
      {
        id: '102',
        type: 'payout',
        status: 'confirmed',
        amountNano: '500000000',
        direction: 'expense',
        dealId: null,
        channelTitle: null,
        description: 'Withdrawal',
        createdAt: '2026-02-18T11:00:00Z',
      },
    ]);
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
    const detail = {
      id: 'tx-1',
      type: 'payout',
      status: 'confirmed',
      amountNano: '1000000000',
      direction: 'income',
      dealId: '00000000-0000-0000-0000-000000000001',
      channelTitle: null,
      description: 'Escrow released',
      createdAt: '2026-02-18T10:00:00Z',
      txHash: 'abc',
      fromAddress: null,
      toAddress: null,
      commissionNano: null,
    };
    mockGet.mockResolvedValue(detail);

    const result = await fetchTransactionDetail('tx-1');
    expect(result).toEqual(detail);
    expect(mockGet).toHaveBeenCalledWith('/wallet/transactions/tx-1');
  });

  it('propagates errors (no fallback)', async () => {
    mockGet.mockRejectedValue(makeApiError(500));

    await expect(fetchTransactionDetail('tx-1')).rejects.toThrow(ApiError);
  });

  it('falls back to list endpoint when detail endpoint is unavailable', async () => {
    mockGet
      .mockRejectedValueOnce(makeApiError(404))
      .mockResolvedValueOnce({
        items: [
          {
            id: 777,
            dealId: '00000000-0000-0000-0000-000000000777',
            entryType: 'OWNER_PAYOUT',
            debitNano: 0,
            creditNano: 1_200_000_000,
            description: 'Escrow released',
            createdAt: '2026-02-18T12:00:00Z',
          },
        ],
        nextCursor: null,
        hasNext: false,
      });

    const result = await fetchTransactionDetail('777');

    expect(result).toEqual({
      id: '777',
      type: 'payout',
      status: 'confirmed',
      amountNano: '1200000000',
      direction: 'income',
      dealId: '00000000-0000-0000-0000-000000000777',
      channelTitle: null,
      description: 'Escrow released',
      createdAt: '2026-02-18T12:00:00Z',
      txHash: null,
      fromAddress: null,
      toAddress: null,
      commissionNano: null,
    });
  });
});

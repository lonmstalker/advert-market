import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook, waitFor } from '@/test/test-utils';

const mockFetchTransactionDetail = vi.fn();

vi.mock('../../api/wallet-api', () => ({
  fetchTransactionDetail: (...args: unknown[]) => mockFetchTransactionDetail(...args),
}));

import { useTransactionDetail } from '../useTransactionDetail';

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

const mockDetail = {
  id: 'tx-1',
  type: 'escrow_deposit',
  status: 'confirmed',
  amountNano: '5000000000',
  direction: 'expense',
  dealId: 'deal-1',
  channelTitle: 'Crypto News Daily',
  description: 'Escrow deposit',
  createdAt: '2026-02-14T10:00:00Z',
  txHash: 'abc123',
  fromAddress: 'EQBvW8Z5',
  toAddress: 'EQAo92DY',
  commissionNano: '250000000',
};

describe('useTransactionDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches detail by txId', async () => {
    mockFetchTransactionDetail.mockResolvedValue(mockDetail);
    const { result } = renderHook(() => useTransactionDetail('tx-1'), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.data).toEqual(mockDetail);
    expect(mockFetchTransactionDetail).toHaveBeenCalledWith('tx-1');
  });

  it('is disabled when txId is empty', () => {
    mockFetchTransactionDetail.mockResolvedValue(mockDetail);
    const { result } = renderHook(() => useTransactionDetail(''), { wrapper: createWrapper() });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.data).toBeUndefined();
    expect(mockFetchTransactionDetail).not.toHaveBeenCalled();
  });

  it('sets isError on failure', async () => {
    mockFetchTransactionDetail.mockRejectedValue(new Error('Not found'));
    const { result } = renderHook(() => useTransactionDetail('tx-999'), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook, waitFor } from '@/test/test-utils';

const mockFetchTransactions = vi.fn();

vi.mock('../../api/wallet-api', () => ({
  fetchTransactions: (...args: unknown[]) => mockFetchTransactions(...args),
}));

import { useTransactions } from '../useTransactions';

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

const mockPage = {
  items: [
    {
      id: 'tx-1',
      type: 'payout',
      status: 'confirmed',
      amountNano: '1000000000',
      direction: 'income',
      dealId: null,
      channelTitle: null,
      description: 'Test',
      createdAt: '2026-02-14T10:00:00Z',
    },
  ],
  nextCursor: 'tx-2',
  hasNext: true,
};

describe('useTransactions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('starts in loading state', () => {
    mockFetchTransactions.mockReturnValue(new Promise(() => {}));
    const { result } = renderHook(() => useTransactions(), { wrapper: createWrapper() });
    expect(result.current.isLoading).toBe(true);
  });

  it('fetches first page', async () => {
    mockFetchTransactions.mockResolvedValue(mockPage);
    const { result } = renderHook(() => useTransactions(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.data?.pages[0].items).toHaveLength(1);
  });

  it('returns hasNextPage from response', async () => {
    mockFetchTransactions.mockResolvedValue(mockPage);
    const { result } = renderHook(() => useTransactions(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.hasNextPage).toBe(true);
  });

  it('passes filter params to query', async () => {
    mockFetchTransactions.mockResolvedValue({ items: [], nextCursor: null, hasNext: false });
    renderHook(() => useTransactions({ type: 'payout' }), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(mockFetchTransactions).toHaveBeenCalledWith(expect.objectContaining({ type: 'payout' }));
    });
  });
});

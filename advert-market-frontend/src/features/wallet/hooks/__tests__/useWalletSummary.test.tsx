import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook, waitFor } from '@/test/test-utils';

const mockFetchWalletSummary = vi.fn();

vi.mock('../../api/wallet-api', () => ({
  fetchWalletSummary: () => mockFetchWalletSummary(),
}));

import { useWalletSummary } from '../useWalletSummary';

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

const mockSummary = {
  earnedTotalNano: '10000000000',
  inEscrowNano: '5000000000',
  spentTotalNano: '0',
  activeEscrowNano: '0',
  activeDealsCount: 1,
  completedDealsCount: 2,
};

describe('useWalletSummary', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('starts in loading state', () => {
    mockFetchWalletSummary.mockReturnValue(new Promise(() => {}));
    const { result } = renderHook(() => useWalletSummary(), { wrapper: createWrapper() });
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();
  });

  it('fetches summary successfully', async () => {
    mockFetchWalletSummary.mockResolvedValue(mockSummary);
    const { result } = renderHook(() => useWalletSummary(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.data).toEqual(mockSummary);
  });

  it('sets isError on API failure', async () => {
    mockFetchWalletSummary.mockRejectedValue(new Error('Server error'));
    const { result } = renderHook(() => useWalletSummary(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});

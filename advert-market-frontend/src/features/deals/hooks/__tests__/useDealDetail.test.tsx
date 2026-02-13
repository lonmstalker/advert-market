import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { mockDeals, mockDealTimelines } from '@/test/mocks/data';
import { server } from '@/test/mocks/server';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

import { useDealDetail } from '../useDealDetail';

const API_BASE = '/api/v1';

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
    },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe('useDealDetail', () => {
  it('starts in loading state', () => {
    const { result } = renderHook(() => useDealDetail('deal-1'), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(true);
    expect(result.current.deal).toBeUndefined();
    expect(result.current.timeline).toBeUndefined();
  });

  it('fetches deal data successfully', async () => {
    const { result } = renderHook(() => useDealDetail('deal-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.deal).toBeDefined();
    expect(result.current.deal?.id).toBe('deal-1');
    expect(result.current.deal?.status).toBe('OFFER_PENDING');
    expect(result.current.deal?.channelTitle).toBe('Tech Digest');
    expect(result.current.isError).toBe(false);
  });

  it('fetches timeline after deal is loaded', async () => {
    const { result } = renderHook(() => useDealDetail('deal-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.timeline).toBeDefined();
    });

    expect(result.current.timeline?.events).toHaveLength(mockDealTimelines['deal-1'].events.length);
    expect(result.current.timeline?.events[0].type).toBe('CREATED');
  });

  it('sets isError when deal fetch fails', async () => {
    server.use(
      http.get(`${API_BASE}/deals/:dealId`, () => {
        return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
      }),
    );

    const { result } = renderHook(() => useDealDetail('nonexistent-deal'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.deal).toBeUndefined();
    expect(result.current.isLoading).toBe(false);
  });

  it('does not fetch timeline when deal query fails', async () => {
    server.use(
      http.get(`${API_BASE}/deals/:dealId`, () => {
        return HttpResponse.json({ type: 'about:blank', title: 'Internal Server Error', status: 500 }, { status: 500 });
      }),
    );

    const { result } = renderHook(() => useDealDetail('fail-deal'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.timeline).toBeUndefined();
  });

  it('returns correct data shape for a deal with full timeline', async () => {
    const { result } = renderHook(() => useDealDetail('deal-7'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.timeline).toBeDefined();
    });

    expect(result.current.deal?.status).toBe('COMPLETED_RELEASED');
    expect(result.current.timeline?.events).toHaveLength(mockDealTimelines['deal-7'].events.length);
  });

  it('provides a refetch function that works', async () => {
    const { result } = renderHook(() => useDealDetail('deal-2'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.deal).toBeDefined();
    });

    expect(typeof result.current.refetch).toBe('function');

    // Call refetch -- should not throw
    result.current.refetch();
  });

  it('returns empty timeline events for a deal without timeline data', async () => {
    server.use(
      http.get(`${API_BASE}/deals/:dealId`, () => {
        return HttpResponse.json(mockDeals[0]);
      }),
      http.get(`${API_BASE}/deals/:dealId/timeline`, () => {
        return HttpResponse.json({ events: [] });
      }),
    );

    const { result } = renderHook(() => useDealDetail('deal-no-timeline'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.timeline).toBeDefined();
    });

    expect(result.current.timeline?.events).toEqual([]);
  });
});

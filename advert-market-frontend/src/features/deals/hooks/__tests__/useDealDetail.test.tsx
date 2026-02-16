import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it } from 'vitest';
import { server } from '@/test/mocks/server';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

import { useDealDetail } from '../useDealDetail';

const API_BASE = '/api/v1';

const dealDetail = {
  id: 'deal-1',
  channelId: 2,
  advertiserId: 1,
  ownerId: 2,
  status: 'OFFER_PENDING' as const,
  amountNano: 3_000_000_000,
  commissionRateBp: 200,
  commissionNano: 60_000_000,
  deadlineAt: null,
  createdAt: '2026-01-01T00:00:00Z',
  version: 1,
  timeline: [
    {
      id: 10,
      eventType: 'DEAL_STATE_CHANGED',
      fromStatus: 'DRAFT' as const,
      toStatus: 'OFFER_PENDING' as const,
      actorId: 1,
      createdAt: '2026-01-01T00:00:00Z',
    },
  ],
};

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
  beforeEach(() => {
    server.use(
      http.get(`${API_BASE}/deals/:dealId`, () => {
        return HttpResponse.json(dealDetail);
      }),
      http.get(`${API_BASE}/deals/:dealId/timeline`, () => {
        return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
      }),
    );
  });

  it('starts in loading state', () => {
    const { result } = renderHook(() => useDealDetail('deal-1'), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(true);
    expect(result.current.deal).toBeUndefined();
    expect(result.current.timeline).toBeUndefined();
  });

  it('reads timeline from deal detail response', async () => {
    const { result } = renderHook(() => useDealDetail('deal-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.deal?.id).toBe('deal-1');
    expect(result.current.deal?.status).toBe('OFFER_PENDING');
    expect(result.current.timeline).toHaveLength(1);
    expect(result.current.timeline?.[0]?.toStatus).toBe('OFFER_PENDING');
  });

  it('does not depend on /deals/{id}/timeline endpoint', async () => {
    const { result } = renderHook(() => useDealDetail('deal-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.deal).toBeDefined();
    });

    expect(result.current.isError).toBe(false);
    expect(result.current.timeline).toEqual(dealDetail.timeline);
  });

  it('sets error state when detail request fails', async () => {
    server.use(
      http.get(`${API_BASE}/deals/:dealId`, () => {
        return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
      }),
    );

    const { result } = renderHook(() => useDealDetail('missing-deal'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.deal).toBeUndefined();
    expect(result.current.timeline).toBeUndefined();
  });

  it('refetch delegates to detail query', async () => {
    const { result } = renderHook(() => useDealDetail('deal-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.deal).toBeDefined();
    });

    expect(typeof result.current.refetch).toBe('function');
    await result.current.refetch();
  });
});

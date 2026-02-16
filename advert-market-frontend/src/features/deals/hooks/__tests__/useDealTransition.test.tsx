import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it } from 'vitest';

const mockTransitionDeal = vi.fn();
const mockShowSuccess = vi.fn();
const mockShowError = vi.fn();
const mockNotificationOccurred = vi.fn();

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
  isHapticFeedbackSupported: vi.fn(() => false),
}));

vi.mock('../../api/deals', () => ({
  transitionDeal: (...args: unknown[]) => mockTransitionDeal(...args),
}));

vi.mock('@/shared/hooks/use-toast', () => ({
  useToast: () => ({
    showSuccess: mockShowSuccess,
    showError: mockShowError,
    showToast: vi.fn(),
    showInfo: vi.fn(),
  }),
}));

vi.mock('@/shared/hooks/use-haptic', () => ({
  useHaptic: () => ({
    impactOccurred: vi.fn(),
    notificationOccurred: mockNotificationOccurred,
    selectionChanged: vi.fn(),
  }),
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: 'en', changeLanguage: vi.fn() },
  }),
}));

import type { DealDetailDto } from '../../types/deal';
import { useDealTransition } from '../useDealTransition';

function createQueryClient(gcTime = 0) {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime },
      mutations: { retry: false },
    },
  });
}

function createWrapper(queryClient?: QueryClient) {
  const qc = queryClient ?? createQueryClient();
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
  };
}

const MOCK_DETAIL: DealDetailDto = {
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
};

describe('useDealTransition', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('exposes transition function and pending flag', () => {
    const { result } = renderHook(() => useDealTransition('deal-1'), {
      wrapper: createWrapper(),
    });

    expect(typeof result.current.transition).toBe('function');
    expect(result.current.isPending).toBe(false);
    expect((result.current as { negotiate?: unknown }).negotiate).toBeUndefined();
  });

  it('calls transitionDeal with targetStatus request', async () => {
    mockTransitionDeal.mockResolvedValueOnce({ status: 'SUCCESS', newStatus: 'ACCEPTED', currentStatus: null });

    const { result } = renderHook(() => useDealTransition('deal-1'), {
      wrapper: createWrapper(),
    });

    result.current.transition({ targetStatus: 'ACCEPTED' });

    await waitFor(() => {
      expect(mockTransitionDeal).toHaveBeenCalledWith('deal-1', { targetStatus: 'ACCEPTED' });
    });
  });

  it('shows success toast and haptic on successful transition', async () => {
    mockTransitionDeal.mockResolvedValueOnce({ status: 'SUCCESS', newStatus: 'ACCEPTED', currentStatus: null });

    const { result } = renderHook(() => useDealTransition('deal-1'), {
      wrapper: createWrapper(),
    });

    result.current.transition({ targetStatus: 'ACCEPTED' });

    await waitFor(() => {
      expect(mockShowSuccess).toHaveBeenCalledWith('deals.transition.success');
    });

    expect(mockNotificationOccurred).toHaveBeenCalledWith('success');
  });

  it('shows error toast and rolls back optimistic state on failure', async () => {
    const queryClient = createQueryClient(300_000);
    queryClient.setQueryData(['deals', 'detail', 'deal-1'], MOCK_DETAIL);

    mockTransitionDeal.mockRejectedValueOnce(new Error('Server error'));

    const { result } = renderHook(() => useDealTransition('deal-1'), {
      wrapper: createWrapper(queryClient),
    });

    result.current.transition({ targetStatus: 'ACCEPTED' });

    await waitFor(() => {
      expect(mockShowError).toHaveBeenCalledWith('deals.transition.error');
    });

    const data = queryClient.getQueryData<DealDetailDto>(['deals', 'detail', 'deal-1']);
    expect(data?.status).toBe('OFFER_PENDING');
  });

  it('invalidates detail and lists queries after transition settles', async () => {
    mockTransitionDeal.mockResolvedValueOnce({ status: 'SUCCESS', newStatus: 'ACCEPTED', currentStatus: null });

    const queryClient = createQueryClient();
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    function Wrapper({ children }: { children: ReactNode }) {
      return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    const { result } = renderHook(() => useDealTransition('deal-42'), {
      wrapper: Wrapper,
    });

    result.current.transition({ targetStatus: 'ACCEPTED' });

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: ['deals', 'detail', 'deal-42'],
      });
    });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: ['deals', 'list'],
    });
    expect(invalidateSpy).not.toHaveBeenCalledWith({
      queryKey: ['deals', 'detail', 'deal-42', 'timeline'],
    });
  });

  it('optimistically updates only for user-initiated non-financial transitions', async () => {
    const queryClient = createQueryClient(300_000);
    queryClient.setQueryData(['deals', 'detail', 'deal-1'], MOCK_DETAIL);

    let resolveTransition: (value: unknown) => void;
    mockTransitionDeal.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveTransition = resolve;
      }),
    );

    const { result } = renderHook(() => useDealTransition('deal-1'), {
      wrapper: createWrapper(queryClient),
    });

    result.current.transition({ targetStatus: 'ACCEPTED' });

    await waitFor(() => {
      const data = queryClient.getQueryData<DealDetailDto>(['deals', 'detail', 'deal-1']);
      expect(data?.status).toBe('ACCEPTED');
    });

    resolveTransition?.({ status: 'SUCCESS', newStatus: 'ACCEPTED', currentStatus: null });
  });

  it('does not apply optimistic update for financial/system transitions', async () => {
    const queryClient = createQueryClient(300_000);
    const fundedDeal: DealDetailDto = { ...MOCK_DETAIL, status: 'AWAITING_PAYMENT' };
    queryClient.setQueryData(['deals', 'detail', 'deal-1'], fundedDeal);

    let resolveTransition: (value: unknown) => void;
    mockTransitionDeal.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveTransition = resolve;
      }),
    );

    const { result } = renderHook(() => useDealTransition('deal-1'), {
      wrapper: createWrapper(queryClient),
    });

    result.current.transition({ targetStatus: 'FUNDED' });

    await waitFor(() => {
      expect(mockTransitionDeal).toHaveBeenCalled();
    });

    const data = queryClient.getQueryData<DealDetailDto>(['deals', 'detail', 'deal-1']);
    expect(data?.status).toBe('AWAITING_PAYMENT');

    resolveTransition?.({ status: 'SUCCESS', newStatus: 'FUNDED', currentStatus: null });
  });
});

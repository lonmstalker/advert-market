import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';

const mockTransitionDeal = vi.fn();
const mockNegotiateDeal = vi.fn();
const mockShowSuccess = vi.fn();
const mockShowError = vi.fn();
const mockNotificationOccurred = vi.fn();

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
  isHapticFeedbackSupported: vi.fn(() => false),
}));

vi.mock('../../api/deals', () => ({
  transitionDeal: (...args: unknown[]) => mockTransitionDeal(...args),
  negotiateDeal: (...args: unknown[]) => mockNegotiateDeal(...args),
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

import type { Deal } from '../../types/deal';
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

const MOCK_DEAL: Deal = {
  id: 'deal-1',
  status: 'OFFER_PENDING',
  channelId: 1,
  channelTitle: 'Test',
  channelUsername: 'test',
  postType: 'native',
  priceNano: 1000000000,
  durationHours: null,
  advertiserId: 1,
  ownerId: 2,
  message: null,
  deadlineAt: null,
  role: 'ADVERTISER',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('useDealTransition', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns transition, negotiate functions and isPending=false initially', () => {
    const { result } = renderHook(() => useDealTransition('deal-1'), {
      wrapper: createWrapper(),
    });

    expect(typeof result.current.transition).toBe('function');
    expect(typeof result.current.negotiate).toBe('function');
    expect(result.current.isPending).toBe(false);
  });

  describe('transition mutation', () => {
    it('calls transitionDeal with dealId and request', async () => {
      mockTransitionDeal.mockResolvedValueOnce({ id: 'deal-1', status: 'ACCEPTED' });

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(),
      });

      result.current.transition({ action: 'ACCEPT' });

      await waitFor(() => {
        expect(mockTransitionDeal).toHaveBeenCalledWith('deal-1', { action: 'ACCEPT' });
      });
    });

    it('shows success toast and haptic on successful transition', async () => {
      mockTransitionDeal.mockResolvedValueOnce({ id: 'deal-1', status: 'ACCEPTED' });

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(),
      });

      result.current.transition({ action: 'ACCEPT' });

      await waitFor(() => {
        expect(mockShowSuccess).toHaveBeenCalledWith('deals.transition.success');
      });

      expect(mockNotificationOccurred).toHaveBeenCalledWith('success');
    });

    it('shows error toast and haptic on failed transition', async () => {
      mockTransitionDeal.mockRejectedValueOnce(new Error('API error'));

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(),
      });

      result.current.transition({ action: 'ACCEPT' });

      await waitFor(() => {
        expect(mockShowError).toHaveBeenCalledWith('deals.transition.error');
      });

      expect(mockNotificationOccurred).toHaveBeenCalledWith('error');
    });

    it('passes message in transition request', async () => {
      mockTransitionDeal.mockResolvedValueOnce({ id: 'deal-1', status: 'CANCELLED' });

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(),
      });

      result.current.transition({ action: 'CANCEL', message: 'Changed my mind' });

      await waitFor(() => {
        expect(mockTransitionDeal).toHaveBeenCalledWith('deal-1', {
          action: 'CANCEL',
          message: 'Changed my mind',
        });
      });
    });
  });

  describe('negotiate mutation', () => {
    it('calls negotiateDeal with dealId and request', async () => {
      mockNegotiateDeal.mockResolvedValueOnce({ id: 'deal-1', status: 'NEGOTIATING' });

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(),
      });

      result.current.negotiate({ priceNano: 5000000000 });

      await waitFor(() => {
        expect(mockNegotiateDeal).toHaveBeenCalledWith('deal-1', { priceNano: 5000000000 });
      });
    });

    it('shows success toast and haptic on successful negotiation', async () => {
      mockNegotiateDeal.mockResolvedValueOnce({ id: 'deal-1', status: 'NEGOTIATING' });

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(),
      });

      result.current.negotiate({ priceNano: 5000000000 });

      await waitFor(() => {
        expect(mockShowSuccess).toHaveBeenCalledWith('deals.transition.negotiateSuccess');
      });

      expect(mockNotificationOccurred).toHaveBeenCalledWith('success');
    });

    it('shows error toast and haptic on failed negotiation', async () => {
      mockNegotiateDeal.mockRejectedValueOnce(new Error('Network error'));

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(),
      });

      result.current.negotiate({ priceNano: 5000000000 });

      await waitFor(() => {
        expect(mockShowError).toHaveBeenCalledWith('deals.transition.negotiateError');
      });

      expect(mockNotificationOccurred).toHaveBeenCalledWith('error');
    });

    it('passes message in negotiate request', async () => {
      mockNegotiateDeal.mockResolvedValueOnce({ id: 'deal-1', status: 'NEGOTIATING' });

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(),
      });

      result.current.negotiate({ priceNano: 3000000000, message: 'Lower price please' });

      await waitFor(() => {
        expect(mockNegotiateDeal).toHaveBeenCalledWith('deal-1', {
          priceNano: 3000000000,
          message: 'Lower price please',
        });
      });
    });
  });

  describe('isPending', () => {
    it('is true while transition is in progress', async () => {
      let resolveTransition: (value: unknown) => void;
      const transitionPromise = new Promise((resolve) => {
        resolveTransition = resolve;
      });
      mockTransitionDeal.mockReturnValueOnce(transitionPromise);

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(),
      });

      result.current.transition({ action: 'ACCEPT' });

      await waitFor(() => {
        expect(result.current.isPending).toBe(true);
      });

      resolveTransition?.({ id: 'deal-1', status: 'ACCEPTED' });

      await waitFor(() => {
        expect(result.current.isPending).toBe(false);
      });
    });

    it('is true while negotiation is in progress', async () => {
      let resolveNegotiate: (value: unknown) => void;
      const negotiatePromise = new Promise((resolve) => {
        resolveNegotiate = resolve;
      });
      mockNegotiateDeal.mockReturnValueOnce(negotiatePromise);

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(),
      });

      result.current.negotiate({ priceNano: 1000000000 });

      await waitFor(() => {
        expect(result.current.isPending).toBe(true);
      });

      resolveNegotiate?.({ id: 'deal-1', status: 'NEGOTIATING' });

      await waitFor(() => {
        expect(result.current.isPending).toBe(false);
      });
    });
  });

  describe('query invalidation', () => {
    it('invalidates deal detail, timeline and lists after successful transition', async () => {
      mockTransitionDeal.mockResolvedValueOnce({ id: 'deal-42', status: 'ACCEPTED' });

      const queryClient = createQueryClient();
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
      }

      const { result } = renderHook(() => useDealTransition('deal-42'), {
        wrapper: Wrapper,
      });

      result.current.transition({ action: 'ACCEPT' });

      await waitFor(() => {
        expect(invalidateSpy).toHaveBeenCalledWith({
          queryKey: ['deals', 'detail', 'deal-42'],
        });
      });

      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: ['deals', 'detail', 'deal-42', 'timeline'],
      });
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: ['deals', 'list'],
      });
    });

    it('invalidates queries after successful negotiation', async () => {
      mockNegotiateDeal.mockResolvedValueOnce({ id: 'deal-42', status: 'NEGOTIATING' });

      const queryClient = createQueryClient();
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
      }

      const { result } = renderHook(() => useDealTransition('deal-42'), {
        wrapper: Wrapper,
      });

      result.current.negotiate({ priceNano: 2000000000 });

      await waitFor(() => {
        expect(invalidateSpy).toHaveBeenCalledWith({
          queryKey: ['deals', 'detail', 'deal-42'],
        });
      });

      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: ['deals', 'detail', 'deal-42', 'timeline'],
      });
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: ['deals', 'list'],
      });
    });
  });

  describe('optimistic updates', () => {
    it('optimistically updates deal status on non-financial transition', async () => {
      const queryClient = createQueryClient(300_000);
      queryClient.setQueryData(['deals', 'detail', 'deal-1'], MOCK_DEAL);

      let resolveTransition: (value: unknown) => void;
      mockTransitionDeal.mockReturnValueOnce(
        new Promise((resolve) => {
          resolveTransition = resolve;
        }),
      );

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(queryClient),
      });

      result.current.transition({ action: 'ACCEPT' });

      await waitFor(() => {
        const data = queryClient.getQueryData<Deal>(['deals', 'detail', 'deal-1']);
        expect(data?.status).toBe('ACCEPTED');
      });

      expect(mockNotificationOccurred).toHaveBeenCalledWith('success');

      resolveTransition?.({ ...MOCK_DEAL, status: 'ACCEPTED' });
    });

    it('fires haptic success in onMutate (before server response)', async () => {
      const queryClient = createQueryClient(300_000);
      queryClient.setQueryData(['deals', 'detail', 'deal-1'], MOCK_DEAL);

      let resolveTransition: (value: unknown) => void;
      mockTransitionDeal.mockReturnValueOnce(
        new Promise((resolve) => {
          resolveTransition = resolve;
        }),
      );

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(queryClient),
      });

      result.current.transition({ action: 'ACCEPT' });

      await waitFor(() => {
        expect(mockNotificationOccurred).toHaveBeenCalledWith('success');
      });

      expect(mockTransitionDeal).toHaveBeenCalled();
      resolveTransition?.({ ...MOCK_DEAL, status: 'ACCEPTED' });
    });

    it('rolls back optimistic update on error', async () => {
      const queryClient = createQueryClient(300_000);
      queryClient.setQueryData(['deals', 'detail', 'deal-1'], MOCK_DEAL);

      mockTransitionDeal.mockRejectedValueOnce(new Error('Server error'));

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(queryClient),
      });

      result.current.transition({ action: 'ACCEPT' });

      await waitFor(() => {
        expect(mockShowError).toHaveBeenCalledWith('deals.transition.error');
      });

      const data = queryClient.getQueryData<Deal>(['deals', 'detail', 'deal-1']);
      expect(data?.status).toBe('OFFER_PENDING');
    });

    it('does NOT apply optimistic update for financial statuses', async () => {
      const queryClient = createQueryClient(300_000);
      const financialDeal: Deal = { ...MOCK_DEAL, status: 'AWAITING_PAYMENT' };
      queryClient.setQueryData(['deals', 'detail', 'deal-1'], financialDeal);

      let resolveTransition: (value: unknown) => void;
      mockTransitionDeal.mockReturnValueOnce(
        new Promise((resolve) => {
          resolveTransition = resolve;
        }),
      );

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(queryClient),
      });

      result.current.transition({ action: 'PAY' });

      await waitFor(() => {
        expect(mockTransitionDeal).toHaveBeenCalled();
      });

      const data = queryClient.getQueryData<Deal>(['deals', 'detail', 'deal-1']);
      expect(data?.status).toBe('AWAITING_PAYMENT');

      resolveTransition?.({ ...financialDeal, status: 'FUNDED' });
    });

    it('does NOT apply optimistic update when no cached deal data', async () => {
      mockTransitionDeal.mockResolvedValueOnce({ ...MOCK_DEAL, status: 'ACCEPTED' });

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(),
      });

      result.current.transition({ action: 'ACCEPT' });

      await waitFor(() => {
        expect(mockShowSuccess).toHaveBeenCalled();
      });

      expect(mockNotificationOccurred).toHaveBeenCalledWith('success');
    });

    it('invalidates in onSettled (always, even on error)', async () => {
      const queryClient = createQueryClient(300_000);
      queryClient.setQueryData(['deals', 'detail', 'deal-1'], MOCK_DEAL);
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      mockTransitionDeal.mockRejectedValueOnce(new Error('fail'));

      const { result } = renderHook(() => useDealTransition('deal-1'), {
        wrapper: createWrapper(queryClient),
      });

      result.current.transition({ action: 'ACCEPT' });

      await waitFor(() => {
        expect(invalidateSpy).toHaveBeenCalledWith({
          queryKey: ['deals', 'detail', 'deal-1'],
        });
      });
    });
  });
});

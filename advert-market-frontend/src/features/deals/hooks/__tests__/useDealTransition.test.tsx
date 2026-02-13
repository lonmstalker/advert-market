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

import { useDealTransition } from '../useDealTransition';

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

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

      const queryClient = new QueryClient({
        defaultOptions: {
          queries: { retry: false, gcTime: 0 },
          mutations: { retry: false },
        },
      });
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

      const queryClient = new QueryClient({
        defaultOptions: {
          queries: { retry: false, gcTime: 0 },
          mutations: { retry: false },
        },
      });
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

    it('does not invalidate queries on failed transition', async () => {
      mockTransitionDeal.mockRejectedValueOnce(new Error('fail'));

      const queryClient = new QueryClient({
        defaultOptions: {
          queries: { retry: false, gcTime: 0 },
          mutations: { retry: false },
        },
      });
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
      }

      const { result } = renderHook(() => useDealTransition('deal-42'), {
        wrapper: Wrapper,
      });

      result.current.transition({ action: 'ACCEPT' });

      await waitFor(() => {
        expect(mockShowError).toHaveBeenCalled();
      });

      expect(invalidateSpy).not.toHaveBeenCalled();
    });
  });
});

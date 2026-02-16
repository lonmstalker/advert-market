import { useOnboardingStore } from '@/features/onboarding';
import { act, fireEvent, renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { TourSlideDeal } from './tour-slide-deal';

// Mock motion/react so animations don't keep "exiting" nodes in the DOM indefinitely in jsdom.
vi.mock('motion/react', () => {
  const React = require('react');

  function createMotionComponent(tag: string) {
    return React.forwardRef((props: Record<string, unknown>, ref: unknown) => {
      const { animate, initial, transition, whileHover, whileTap, variants, ...rest } = props;
      const animateStyles = typeof animate === 'object' && animate !== null ? animate : {};
      const mergedStyle = { ...(rest.style as object), ...animateStyles };
      return React.createElement(tag, { ...rest, style: mergedStyle, ref });
    });
  }

  const motionProxy = new Proxy(
    {},
    {
      get(_target: object, prop: string) {
        return createMotionComponent(prop);
      },
    },
  );

  return {
    motion: motionProxy,
    AnimatePresence: ({ children }: { children: React.ReactNode }) => children,
    useReducedMotion: () => false,
  };
});

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

describe('TourSlideDeal', () => {
  beforeEach(() => {
    useOnboardingStore.getState().reset();
  });

  it('renders timeline with 3 macro steps', () => {
    renderWithProviders(<TourSlideDeal primaryRole="advertiser" />);

    expect(screen.getByText('Offer & Negotiation')).toBeInTheDocument();
    expect(screen.getByText('Payment & Creative')).toBeInTheDocument();
    expect(screen.getByText('Publication & Payout')).toBeInTheDocument();
  });

  it('shows Approve button', () => {
    renderWithProviders(<TourSlideDeal primaryRole="advertiser" />);

    expect(screen.getByRole('button', { name: 'Approve' })).toBeInTheDocument();
  });

  it('calls completeTourTask(1) on approve click', async () => {
    const { user } = renderWithProviders(<TourSlideDeal primaryRole="advertiser" />);

    await user.click(screen.getByRole('button', { name: 'Approve' }));

    await waitFor(() => {
      expect(useOnboardingStore.getState().tourTasksCompleted.has(1)).toBe(true);
    });
  });

  it('shows task done message after approve', async () => {
    const { user } = renderWithProviders(<TourSlideDeal primaryRole="advertiser" />);

    await user.click(screen.getByRole('button', { name: 'Approve' }));

    await waitFor(() => {
      expect(screen.getByText(/Creative approved! Moving to publication/)).toBeInTheDocument();
    });
  });

  it('does not auto-reset approved state after a delay', async () => {
    vi.useFakeTimers();
    try {
      renderWithProviders(<TourSlideDeal primaryRole="advertiser" />);

      fireEvent.click(screen.getByRole('button', { name: 'Approve' }));

      expect(screen.getByText(/Creative approved! Moving to publication/)).toBeInTheDocument();

      // Previously the demo auto-reset after 2.5s; keep approved state until user leaves or replays.
      act(() => {
        vi.advanceTimersByTime(3_000);
      });

      expect(screen.getByText(/Creative approved! Moving to publication/)).toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });

  it('switches to allStates view', async () => {
    const { user } = renderWithProviders(<TourSlideDeal primaryRole="advertiser" />);

    await user.click(screen.getByText('All 17 states →'));

    await waitFor(() => {
      expect(screen.getByText('Negotiation')).toBeInTheDocument();
    });
  });

  it('shows all 4 groups in allStates view', async () => {
    const { user } = renderWithProviders(<TourSlideDeal primaryRole="advertiser" />);

    await user.click(screen.getByText('All 17 states →'));

    await waitFor(() => {
      expect(screen.getByText('Negotiation')).toBeInTheDocument();
      expect(screen.getByText('Payment & Creative')).toBeInTheDocument();
      expect(screen.getByText('Publication')).toBeInTheDocument();
      expect(screen.getByText('Special Cases')).toBeInTheDocument();
    });
  });

  it('navigates back from allStates to timeline', async () => {
    const { user } = renderWithProviders(<TourSlideDeal primaryRole="advertiser" />);

    await user.click(screen.getByText('All 17 states →'));
    await waitFor(() => {
      expect(screen.getByText('Negotiation')).toBeInTheDocument();
    });

    await user.click(screen.getByText('← Back to deal'));

    await waitFor(() => {
      expect(screen.getByText('Offer & Negotiation')).toBeInTheDocument();
    });
  });

  it('hides TaskHint after approve', async () => {
    const { user } = renderWithProviders(<TourSlideDeal primaryRole="advertiser" />);

    await user.click(screen.getByRole('button', { name: 'Approve' }));

    await waitFor(() => {
      expect(screen.queryByText(/Tap any deal step/)).not.toBeInTheDocument();
    });
  });

  it('hides TaskHint in allStates view', async () => {
    const { user } = renderWithProviders(<TourSlideDeal primaryRole="advertiser" />);

    await user.click(screen.getByText('All 17 states →'));

    await waitFor(() => {
      expect(screen.queryByText(/Tap any deal step/)).not.toBeInTheDocument();
    });
  });
});

import { useOnboardingStore } from '@/features/onboarding';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { TourSlideDeal } from './tour-slide-deal';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

describe('TourSlideDeal', () => {
  beforeEach(() => {
    useOnboardingStore.getState().reset();
  });

  it('renders timeline with 3 macro steps', () => {
    renderWithProviders(<TourSlideDeal />);

    expect(screen.getByText('Offer & Negotiation')).toBeInTheDocument();
    expect(screen.getByText('Payment & Creative')).toBeInTheDocument();
    expect(screen.getByText('Publication & Payout')).toBeInTheDocument();
  });

  it('shows Approve button', () => {
    renderWithProviders(<TourSlideDeal />);

    expect(screen.getByRole('button', { name: 'Approve' })).toBeInTheDocument();
  });

  it('calls completeTourTask(1) on approve click', async () => {
    const { user } = renderWithProviders(<TourSlideDeal />);

    await user.click(screen.getByRole('button', { name: 'Approve' }));

    await waitFor(() => {
      expect(useOnboardingStore.getState().tourTasksCompleted.has(1)).toBe(true);
    });
  });

  it('shows task done message after approve', async () => {
    const { user } = renderWithProviders(<TourSlideDeal />);

    await user.click(screen.getByRole('button', { name: 'Approve' }));

    await waitFor(() => {
      expect(screen.getByText(/Creative approved! Moving to publication/)).toBeInTheDocument();
    });
  });

  it('switches to allStates view', async () => {
    const { user } = renderWithProviders(<TourSlideDeal />);

    await user.click(screen.getByText('All 17 states →'));

    await waitFor(() => {
      expect(screen.getByText('Negotiation')).toBeInTheDocument();
    });
  });

  it('shows all 4 groups in allStates view', async () => {
    const { user } = renderWithProviders(<TourSlideDeal />);

    await user.click(screen.getByText('All 17 states →'));

    await waitFor(() => {
      expect(screen.getByText('Negotiation')).toBeInTheDocument();
      expect(screen.getByText('Payment & Creative')).toBeInTheDocument();
      expect(screen.getByText('Publication')).toBeInTheDocument();
      expect(screen.getByText('Special Cases')).toBeInTheDocument();
    });
  });

  it('navigates back from allStates to timeline', async () => {
    const { user } = renderWithProviders(<TourSlideDeal />);

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
    const { user } = renderWithProviders(<TourSlideDeal />);

    await user.click(screen.getByRole('button', { name: 'Approve' }));

    await waitFor(() => {
      expect(screen.queryByText(/Tap any deal step/)).not.toBeInTheDocument();
    });
  });

  it('hides TaskHint in allStates view', async () => {
    const { user } = renderWithProviders(<TourSlideDeal />);

    await user.click(screen.getByText('All 17 states →'));

    await waitFor(() => {
      expect(screen.queryByText(/Tap any deal step/)).not.toBeInTheDocument();
    });
  });
});

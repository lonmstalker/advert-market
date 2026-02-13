import { useOnboardingStore } from '@/features/onboarding';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { TourSlideWallet } from './tour-slide-wallet';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

describe('TourSlideWallet', () => {
  beforeEach(() => {
    useOnboardingStore.getState().reset();
  });

  it('renders details view initially', () => {
    renderWithProviders(<TourSlideWallet />);

    expect(screen.getByText('Escrow')).toBeInTheDocument();
    expect(screen.getAllByText('5.00 TON').length).toBeGreaterThanOrEqual(1);
  });

  it('shows escrow flow on escrow click', async () => {
    const { user } = renderWithProviders(<TourSlideWallet />);

    await user.click(screen.getByText('Escrow'));

    await waitFor(() => {
      expect(screen.getByText('Advertiser')).toBeInTheDocument();
    });
  });

  it('calls completeTourTask(2) on escrow click', async () => {
    const { user } = renderWithProviders(<TourSlideWallet />);

    await user.click(screen.getByText('Escrow'));

    await waitFor(() => {
      expect(useOnboardingStore.getState().tourTasksCompleted.has(2)).toBe(true);
    });
  });

  it('shows task done in flow view', async () => {
    const { user } = renderWithProviders(<TourSlideWallet />);

    await user.click(screen.getByText('Escrow'));

    await waitFor(() => {
      expect(screen.getByText(/secure escrow works/i)).toBeInTheDocument();
    });
  });

  it('navigates to policy view', async () => {
    const { user } = renderWithProviders(<TourSlideWallet />);

    await user.click(screen.getByText('Escrow'));
    await waitFor(() => {
      expect(screen.getByText('Advertiser')).toBeInTheDocument();
    });

    await user.click(screen.getByText('How are payments confirmed? →'));

    await waitFor(() => {
      expect(screen.getByText('Confirmation Limits')).toBeInTheDocument();
    });
  });

  it('navigates back from policy to flow', async () => {
    const { user } = renderWithProviders(<TourSlideWallet />);

    await user.click(screen.getByText('Escrow'));
    await waitFor(() => {
      expect(screen.getByText('Advertiser')).toBeInTheDocument();
    });

    await user.click(screen.getByText('How are payments confirmed? →'));
    await waitFor(() => {
      expect(screen.getByText('Confirmation Limits')).toBeInTheDocument();
    });

    await user.click(screen.getByText('← Back to escrow'));

    await waitFor(() => {
      expect(screen.getByText('Advertiser')).toBeInTheDocument();
    });
  });

  it('hides TaskHint in flow view', async () => {
    const { user } = renderWithProviders(<TourSlideWallet />);

    await user.click(screen.getByText('Escrow'));

    await waitFor(() => {
      expect(screen.queryByText(/Tap on 'Escrow'/)).not.toBeInTheDocument();
    });
  });
});

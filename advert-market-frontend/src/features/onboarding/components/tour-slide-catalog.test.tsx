import { useOnboardingStore } from '@/features/onboarding';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { TourSlideCatalog } from './tour-slide-catalog';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

describe('TourSlideCatalog', () => {
  beforeEach(() => {
    useOnboardingStore.getState().reset();
  });

  it('renders channel list initially', () => {
    renderWithProviders(<TourSlideCatalog />);

    expect(screen.getByText('Crypto News Daily')).toBeInTheDocument();
    expect(screen.getByText('Tech Digest')).toBeInTheDocument();
    expect(screen.getByText('AI Weekly')).toBeInTheDocument();
  });

  it('shows channel detail on channel click', async () => {
    const { user } = renderWithProviders(<TourSlideCatalog />);

    await user.click(screen.getByText('Crypto News Daily'));

    await waitFor(() => {
      expect(screen.getByText('Post Price')).toBeInTheDocument();
    });
  });

  it('calls completeTourTask(0) on channel click', async () => {
    const { user } = renderWithProviders(<TourSlideCatalog />);

    await user.click(screen.getByText('Crypto News Daily'));

    await waitFor(() => {
      expect(useOnboardingStore.getState().tourTasksCompleted.has(0)).toBe(true);
    });
  });

  it('shows task done message in detail view', async () => {
    const { user } = renderWithProviders(<TourSlideCatalog />);

    await user.click(screen.getByText('Crypto News Daily'));

    await waitFor(() => {
      expect(screen.getByText(/channel details/i)).toBeInTheDocument();
    });
  });

  it('navigates back to list on back click', async () => {
    const { user } = renderWithProviders(<TourSlideCatalog />);

    await user.click(screen.getByText('Crypto News Daily'));
    await waitFor(() => {
      expect(screen.getByText('Post Price')).toBeInTheDocument();
    });

    await user.click(screen.getByText('← Back to list'));

    await waitFor(() => {
      expect(screen.getByText('Tech Digest')).toBeInTheDocument();
    });
  });

  it('hides TaskHint in detail view', async () => {
    const { user } = renderWithProviders(<TourSlideCatalog />);

    await user.click(screen.getByText('Crypto News Daily'));

    await waitFor(() => {
      expect(screen.queryByText(/Tap on 'Crypto News Daily'/)).not.toBeInTheDocument();
    });
  });

  it('shows TaskHint in list view', () => {
    renderWithProviders(<TourSlideCatalog />);

    expect(screen.getByText(/Tap on 'Crypto News Daily'/)).toBeInTheDocument();
  });
});

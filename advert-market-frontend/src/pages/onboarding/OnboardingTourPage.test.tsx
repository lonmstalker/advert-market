import { Route, Routes } from 'react-router';
import { useOnboardingStore } from '@/features/onboarding';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import OnboardingTourPage from './OnboardingTourPage';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

describe('OnboardingTourPage', () => {
  beforeEach(() => {
    useOnboardingStore.getState().reset();
    useOnboardingStore.getState().toggleInterest('advertiser');
    sessionStorage.setItem('access_token', 'test-token');
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  function renderPage() {
    return renderWithProviders(
      <Routes>
        <Route path="/onboarding/tour" element={<OnboardingTourPage />} />
        <Route path="/catalog" element={<div>catalog-page</div>} />
      </Routes>,
      { initialEntries: ['/onboarding/tour'] },
    );
  }

  it('renders 3 tour slides', () => {
    renderPage();
    expect(screen.getByText('Find Channels')).toBeInTheDocument();
    expect(screen.getByText('Secure Deals')).toBeInTheDocument();
    expect(screen.getByText('Manage Placements')).toBeInTheDocument();
  });

  it('renders slide descriptions', () => {
    renderPage();
    expect(screen.getByText(/Channel catalog with filters/)).toBeInTheDocument();
    expect(screen.getByText(/Escrow payments via TON/)).toBeInTheDocument();
    expect(screen.getByText(/Track deal status/)).toBeInTheDocument();
  });

  it('shows Skip button on first slide', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Skip' })).toBeInTheDocument();
  });

  it('navigates to /catalog on Skip click', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: 'Skip' }));
    expect(await screen.findByText('catalog-page')).toBeInTheDocument();
  });

  it('resets onboarding store after mutation', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: 'Skip' }));
    await screen.findByText('catalog-page');
    await waitFor(() => {
      expect(useOnboardingStore.getState().interests.size).toBe(0);
    });
  });

  it('renders slide emojis', () => {
    renderPage();
    expect(screen.getByText('🔍')).toBeInTheDocument();
    expect(screen.getByText('🔒')).toBeInTheDocument();
    expect(screen.getByText('📊')).toBeInTheDocument();
  });
});

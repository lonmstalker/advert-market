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

  it('renders first slide on mount', () => {
    renderPage();
    expect(screen.getByText('Find Channels')).toBeInTheDocument();
    expect(screen.getByText(/Channel catalog with filters/)).toBeInTheDocument();
    expect(screen.getByText('🔍')).toBeInTheDocument();
  });

  it('shows Next button and Skip link on first slide', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Next' })).toBeInTheDocument();
    expect(screen.getByText('Skip')).toBeInTheDocument();
  });

  it('advances to second slide on Next click', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => {
      expect(screen.getByText('Secure Deals')).toBeInTheDocument();
    });
  });

  it('shows Get Started button on last slide', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(screen.getByText('Secure Deals')).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Get Started' })).toBeInTheDocument();
    });
  });

  it('hides Skip link on last slide', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(screen.getByText('Secure Deals')).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => {
      expect(screen.queryByText('Skip')).not.toBeInTheDocument();
    });
  });

  it('navigates to /catalog on Skip click', async () => {
    const { user } = renderPage();
    await user.click(screen.getByText('Skip'));
    expect(await screen.findByText('catalog-page')).toBeInTheDocument();
  });

  it('resets onboarding store after completing', async () => {
    const { user } = renderPage();
    await user.click(screen.getByText('Skip'));
    await screen.findByText('catalog-page');
    await waitFor(() => {
      expect(useOnboardingStore.getState().interests.size).toBe(0);
    });
  });
});

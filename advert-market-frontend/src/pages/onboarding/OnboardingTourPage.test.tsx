import { Route, Routes } from 'react-router';
import { useOnboardingStore } from '@/features/onboarding';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import OnboardingTourPage from './OnboardingTourPage';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

function completeTask(index: number) {
  useOnboardingStore.getState().completeTourTask(index);
}

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
        <Route path="/onboarding/interest" element={<div>interest-page</div>} />
        <Route path="/catalog" element={<div>catalog-page</div>} />
      </Routes>,
      { initialEntries: ['/onboarding/tour'] },
    );
  }

  it('renders first slide with interactive catalog mockup', () => {
    renderPage();
    expect(screen.getByText('Find Channels')).toBeInTheDocument();
    expect(screen.getByText('Crypto News Daily')).toBeInTheDocument();
  });

  it('shows Next button and Skip link on first slide', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Next' })).toBeInTheDocument();
    expect(screen.getByText('Skip')).toBeInTheDocument();
  });

  it('Next button is disabled until task is completed', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('Next button becomes enabled after completing slide task', async () => {
    renderPage();
    completeTask(0);
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled();
    });
  });

  it('advances to second slide on Next click after task', async () => {
    const { user } = renderPage();
    completeTask(0);
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => {
      expect(screen.getByText('Secure Deals')).toBeInTheDocument();
    });
  });

  it('shows Get Started button on last slide', async () => {
    const { user } = renderPage();
    completeTask(0);
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(screen.getByText('Secure Deals')).toBeInTheDocument());
    completeTask(1);
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Get Started' })).toBeInTheDocument();
    });
  });

  it('hides Skip link on last slide', async () => {
    const { user } = renderPage();
    completeTask(0);
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(screen.getByText('Secure Deals')).toBeInTheDocument());
    completeTask(1);
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

  it('redirects to /onboarding/interest when interests are empty', () => {
    useOnboardingStore.getState().reset();
    renderPage();
    expect(screen.getByText('interest-page')).toBeInTheDocument();
  });

  it('shows task required hint when task not completed', () => {
    renderPage();
    expect(screen.getByText('Complete the task to continue')).toBeInTheDocument();
  });

  it('renders pagination dots with ARIA tablist role', () => {
    renderPage();
    expect(screen.getByRole('tablist')).toBeInTheDocument();
    expect(screen.getAllByRole('tab')).toHaveLength(3);
  });
});

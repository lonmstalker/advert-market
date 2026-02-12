import { Route, Routes } from 'react-router';
import { useOnboardingStore } from '@/features/onboarding';
import { renderWithProviders, screen } from '@/test/test-utils';
import OnboardingInterestPage from './OnboardingInterestPage';

describe('OnboardingInterestPage', () => {
  beforeEach(() => {
    useOnboardingStore.getState().reset();
  });

  function renderPage() {
    return renderWithProviders(
      <Routes>
        <Route path="/onboarding/interest" element={<OnboardingInterestPage />} />
        <Route path="/onboarding/tour" element={<div>tour-page</div>} />
      </Routes>,
      { initialEntries: ['/onboarding/interest'] },
    );
  }

  it('renders title and subtitle', () => {
    renderPage();
    expect(screen.getByText('Who are you?')).toBeInTheDocument();
    expect(screen.getByText('Choose your role on the platform')).toBeInTheDocument();
  });

  it('renders 2 interest cards', () => {
    renderPage();
    expect(screen.getByText('Advertiser')).toBeInTheDocument();
    expect(screen.getByText('Channel Owner')).toBeInTheDocument();
  });

  it('Continue button is disabled without selection', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Continue' })).toBeDisabled();
  });

  it('Continue button becomes enabled after selecting an interest', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: /advertiser/i }));
    expect(screen.getByRole('button', { name: 'Continue' })).toBeEnabled();
  });

  it('Continue button becomes disabled after toggling off', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: /advertiser/i }));
    await user.click(screen.getByRole('button', { name: /advertiser/i }));
    expect(screen.getByRole('button', { name: 'Continue' })).toBeDisabled();
  });

  it('navigates to /onboarding/tour on Continue click', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: /advertiser/i }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByText('tour-page')).toBeInTheDocument();
  });
});

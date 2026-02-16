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
    expect(screen.getByText('This will personalize your experience')).toBeInTheDocument();
  });

  it('renders 2 role cards', () => {
    renderPage();
    expect(screen.getByText('Advertiser')).toBeInTheDocument();
    expect(screen.getByText('Channel Owner')).toBeInTheDocument();
  });

  it('Continue button is disabled without selection', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Continue' })).toBeDisabled();
  });

  it('Continue button becomes enabled after selecting a role', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: /advertiser/i }));
    expect(screen.getByRole('button', { name: 'Continue' })).toBeEnabled();
  });

  it('shows preview items when role is selected', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: /advertiser/i }));
    expect(screen.getByText('Find channels')).toBeInTheDocument();
    expect(screen.getByText('Create deals')).toBeInTheDocument();
    expect(screen.getByText('Track results')).toBeInTheDocument();
  });

  it('shows both roles hint when both selected', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: /advertiser/i }));
    await user.click(screen.getByRole('button', { name: /channel owner/i }));
    expect(screen.getByText("We'll start from Catalog. Owner tools stay available in Profile.")).toBeInTheDocument();
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

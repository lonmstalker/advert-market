import { Route, Routes } from 'react-router';
import { renderWithProviders, screen } from '@/test/test-utils';
import OnboardingPage from './OnboardingPage';

describe('OnboardingPage', () => {
  function renderPage() {
    return renderWithProviders(
      <Routes>
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route path="/onboarding/interest" element={<div>interest-page</div>} />
      </Routes>,
      { initialEntries: ['/onboarding'] },
    );
  }

  it('renders welcome title', () => {
    renderPage();
    expect(screen.getByText('Welcome to Ad Market')).toBeInTheDocument();
  });

  it('renders welcome subtitle', () => {
    renderPage();
    expect(screen.getByText('Marketplace for Telegram channel advertising')).toBeInTheDocument();
  });

  it('renders Get Started button', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Get Started' })).toBeInTheDocument();
  });

  it('navigates to /onboarding/interest on button click', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: 'Get Started' }));
    expect(screen.getByText('interest-page')).toBeInTheDocument();
  });
});

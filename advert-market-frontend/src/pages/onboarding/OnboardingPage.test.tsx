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

  it('renders product name', () => {
    renderPage();
    expect(screen.getByText('Ad Market')).toBeInTheDocument();
  });

  it('renders welcome subtitle', () => {
    renderPage();
    expect(screen.getByText('Marketplace for Telegram channel advertising')).toBeInTheDocument();
  });

  it('renders feature highlights', () => {
    renderPage();
    expect(screen.getByText('Channel catalog with filters')).toBeInTheDocument();
    expect(screen.getByText('Escrow payments via TON')).toBeInTheDocument();
    expect(screen.getByText('Deal tracking and reports')).toBeInTheDocument();
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

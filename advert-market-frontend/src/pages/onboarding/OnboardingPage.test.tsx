import { ToastProvider } from '@telegram-tools/ui-kit';
import i18n from 'i18next';
import { Route, Routes } from 'react-router';
import { renderWithProviders, screen } from '@/test/test-utils';
import OnboardingPage from './OnboardingPage';

describe('OnboardingPage', () => {
  function renderPage() {
    return renderWithProviders(
      <ToastProvider>
        <Routes>
          <Route path="/onboarding" element={<OnboardingPage />} />
          <Route path="/onboarding/interest" element={<div>interest-page</div>} />
        </Routes>
      </ToastProvider>,
      { initialEntries: ['/onboarding'] },
    );
  }

  beforeEach(async () => {
    await i18n.changeLanguage('en');
  });

  it('renders product name', () => {
    renderPage();
    expect(screen.getByText('Ad Market')).toBeInTheDocument();
  });

  it('renders welcome subtitle', () => {
    renderPage();
    expect(screen.getByText('Marketplace for Telegram channel advertising')).toBeInTheDocument();
  });

  it('renders feature cards with icons and descriptions', () => {
    renderPage();
    expect(screen.getByText('Channel Catalog')).toBeInTheDocument();
    expect(screen.getByText('150K+ subscribers')).toBeInTheDocument();
    expect(screen.getByText('TON Escrow')).toBeInTheDocument();
    expect(screen.getByText('Protected payments')).toBeInTheDocument();
    expect(screen.getByText('Deal Tracking')).toBeInTheDocument();
    expect(screen.getByText('17-state pipeline')).toBeInTheDocument();
  });

  it('renders time hint', () => {
    renderPage();
    expect(screen.getByText('Takes 30 seconds')).toBeInTheDocument();
  });

  it('renders Get Started button', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Get Started' })).toBeInTheDocument();
  });

  it('opens locale step sheet on Get Started click', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: 'Get Started' }));
    expect(screen.getByText('Language & Currency')).toBeInTheDocument();
  });

  it('navigates to /onboarding/interest after locale Continue', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: 'Get Started' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(screen.getByText('interest-page')).toBeInTheDocument();
  });
});

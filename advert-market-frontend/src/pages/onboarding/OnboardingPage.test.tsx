import { ToastProvider } from '@telegram-tools/ui-kit';
import i18n from 'i18next';
import { Route, Routes } from 'react-router';
import { vi } from 'vitest';
import { updateLanguage, updateSettings } from '@/shared/api/profile';
import ru from '@/shared/i18n/locales/ru.json';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { renderWithProviders, screen } from '@/test/test-utils';
import OnboardingInterestPage from './OnboardingInterestPage';
import OnboardingPage from './OnboardingPage';
import OnboardingTourPage from './OnboardingTourPage';

vi.mock('@/shared/api/profile', () => ({
  updateLanguage: vi.fn(),
  updateSettings: vi.fn(),
}));

function makeProfile(overrides: Record<string, unknown> = {}) {
  return {
    id: 1,
    telegramId: 1,
    username: 'user',
    displayName: 'User',
    languageCode: 'en',
    displayCurrency: 'USD',
    currencyMode: 'AUTO',
    notificationSettings: {
      deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
      financial: { deposits: true, payouts: true, escrow: true },
      disputes: { opened: true, resolved: true },
    },
    onboardingCompleted: false,
    interests: [],
    createdAt: '2026-02-16T00:00:00.000Z',
    ...overrides,
  };
}

describe('OnboardingPage', () => {
  function renderPage() {
    return renderWithProviders(
      <ToastProvider>
        <Routes>
          <Route path="/onboarding" element={<OnboardingPage />} />
          <Route path="/onboarding/interest" element={<OnboardingInterestPage />} />
          <Route path="/onboarding/tour" element={<OnboardingTourPage />} />
        </Routes>
      </ToastProvider>,
      { initialEntries: ['/onboarding'] },
    );
  }

  beforeEach(async () => {
    if (!i18n.hasResourceBundle('ru', 'translation')) {
      i18n.addResourceBundle('ru', 'translation', ru, true, true);
    }
    await i18n.changeLanguage('en');
    vi.mocked(updateLanguage).mockReset();
    vi.mocked(updateSettings).mockReset();
    useSettingsStore.setState({
      languageCode: 'en',
      displayCurrency: 'USD',
      currencyMode: 'AUTO',
      isLoaded: true,
      notificationSettings: {
        deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
        financial: { deposits: true, payouts: true, escrow: true },
        disputes: { opened: true, resolved: true },
      },
    });
  });

  it('shows locale step as the first onboarding screen', () => {
    renderPage();
    expect(screen.getByText('Language & Currency')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Continue' })).toBeInTheDocument();
    expect(screen.queryByText('Ad Market')).not.toBeInTheDocument();
  });

  it('shows welcome section after locale continue', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: 'Continue' }));

    expect(screen.getByText('Ad Market')).toBeInTheDocument();
    expect(screen.getByText('Marketplace for Telegram channel advertising')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Get Started' })).toBeInTheDocument();
  });

  it('applies selected language before welcome/interest screens', async () => {
    vi.mocked(updateLanguage).mockResolvedValue(makeProfile({ languageCode: 'ru', displayCurrency: 'RUB' }));

    const { user } = renderPage();
    await user.click(screen.getByText('Language'));
    await user.click(screen.getByText('Русский'));
    await user.click(screen.getByRole('button', { name: 'Продолжить' }));

    expect(screen.getByText('Маркетплейс рекламы в Telegram-каналах')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Начать' }));
    expect(screen.getByText('Кто вы?')).toBeInTheDocument();
  });

  it('keeps tutorial localized after language selection on first onboarding step', async () => {
    vi.mocked(updateLanguage).mockResolvedValue(makeProfile({ languageCode: 'ru', displayCurrency: 'RUB' }));

    const { user } = renderPage();
    await user.click(screen.getByText('Language'));
    await user.click(screen.getByText('Русский'));
    await user.click(screen.getByRole('button', { name: 'Продолжить' }));
    await user.click(screen.getByRole('button', { name: 'Начать' }));

    expect(screen.getByText('Кто вы?')).toBeInTheDocument();
    await user.click(screen.getByText('Рекламодатель'));
    await user.click(screen.getByRole('button', { name: 'Продолжить' }));

    expect(screen.getByRole('button', { name: 'Далее' })).toBeInTheDocument();
    expect(screen.getByText('Находите каналы')).toBeInTheDocument();
  });
});

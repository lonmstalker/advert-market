import { ToastProvider } from '@telegram-tools/ui-kit';
import i18n from 'i18next';
import { Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { updateLanguage, updateSettings } from '@/features/profile/api/profile-api';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import LocaleCurrencyPage from '../LocaleCurrencyPage';

vi.mock('@/features/profile/api/profile-api', () => ({
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
    onboardingCompleted: true,
    interests: ['advertiser'],
    createdAt: '2026-02-15T00:00:00.000Z',
    ...overrides,
  };
}

function renderPage() {
  return renderWithProviders(
    <ToastProvider>
      <Routes>
        <Route path="/profile/locale-currency" element={<LocaleCurrencyPage />} />
      </Routes>
    </ToastProvider>,
    { initialEntries: ['/profile/locale-currency'] },
  );
}

describe('LocaleCurrencyPage', () => {
  beforeEach(async () => {
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

  it('renders title and subtitle', () => {
    renderPage();
    expect(screen.getByText('Language & Currency')).toBeInTheDocument();
    expect(screen.getByText('You can change it later from profile settings.')).toBeInTheDocument();
  });

  it('updates language through API', async () => {
    vi.mocked(updateLanguage).mockResolvedValue(makeProfile({ languageCode: 'ru', displayCurrency: 'RUB' }));

    const { user } = renderPage();
    await user.click(screen.getByText('Language'));
    await user.click(screen.getByText('Русский'));

    await waitFor(() => {
      expect(updateLanguage).toHaveBeenCalled();
      expect(vi.mocked(updateLanguage).mock.calls[0][0]).toBe('ru');
    });
  });

  it('shows manual microcopy and resets to auto', async () => {
    useSettingsStore.setState({
      languageCode: 'en',
      displayCurrency: 'EUR',
      currencyMode: 'MANUAL',
      isLoaded: true,
      notificationSettings: {
        deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
        financial: { deposits: true, payouts: true, escrow: true },
        disputes: { opened: true, resolved: true },
      },
    });

    vi.mocked(updateSettings).mockResolvedValue(makeProfile({ currencyMode: 'AUTO', displayCurrency: 'USD' }));

    const { user } = renderPage();

    expect(screen.getByText('Currency is selected manually.')).toBeInTheDocument();
    await user.click(screen.getByText('Reset to auto'));

    await waitFor(() => {
      expect(updateSettings).toHaveBeenCalled();
      expect(vi.mocked(updateSettings).mock.calls[0][0]).toEqual({ currencyMode: 'AUTO' });
    });
  });
});

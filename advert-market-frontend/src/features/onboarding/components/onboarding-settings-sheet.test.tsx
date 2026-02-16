import { ToastProvider } from '@telegram-tools/ui-kit';
import i18n from 'i18next';
import { describe, expect, it, vi } from 'vitest';
import { updateLanguage, updateSettings } from '@/features/profile/api/profile-api';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { LocaleCurrencyStepSheet } from './onboarding-settings-sheet';

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
    onboardingCompleted: false,
    interests: [],
    createdAt: '2026-02-15T00:00:00.000Z',
    ...overrides,
  };
}

describe('LocaleCurrencyStepSheet', () => {
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

  it('opens locale step and calls onContinue', async () => {
    const onContinue = vi.fn();

    const { user } = renderWithProviders(
      <ToastProvider>
        <LocaleCurrencyStepSheet open onClose={() => {}} onContinue={onContinue} />
      </ToastProvider>,
    );

    expect(screen.getByText('Language & Currency')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(onContinue).toHaveBeenCalledTimes(1);
  });

  it('persists language change through profile API', async () => {
    const updateLanguageMock = vi.mocked(updateLanguage);
    updateLanguageMock.mockResolvedValue(makeProfile({ languageCode: 'ru', displayCurrency: 'RUB' }));

    const { user } = renderWithProviders(
      <ToastProvider>
        <LocaleCurrencyStepSheet open onClose={() => {}} onContinue={() => {}} />
      </ToastProvider>,
    );

    await user.click(screen.getByText('Language'));
    await user.click(screen.getByText('Русский'));

    await waitFor(() => {
      expect(updateLanguageMock).toHaveBeenCalled();
      expect(updateLanguageMock.mock.calls[0][0]).toBe('ru');
    });
  });

  it('switches to manual mode when selecting explicit currency', async () => {
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

    const updateSettingsMock = vi.mocked(updateSettings);
    updateSettingsMock.mockResolvedValue(makeProfile({ currencyMode: 'MANUAL', displayCurrency: 'EUR' }));

    const { user } = renderWithProviders(
      <ToastProvider>
        <LocaleCurrencyStepSheet open onClose={() => {}} onContinue={() => {}} />
      </ToastProvider>,
    );

    await user.click(screen.getByText('Display Currency'));
    await user.click(screen.getByText('Euro (€)'));

    await waitFor(() => {
      expect(updateSettingsMock).toHaveBeenCalled();
      expect(updateSettingsMock.mock.calls[0][0]).toEqual({
        currencyMode: 'MANUAL',
        displayCurrency: 'EUR',
      });
    });
  });

  it('uses profile language as source of truth when saving language selection', async () => {
    useSettingsStore.getState().setFromProfile(
      makeProfile({
        languageCode: 'ru',
        displayCurrency: 'RUB',
        currencyMode: 'AUTO',
      }),
    );

    const updateLanguageMock = vi.mocked(updateLanguage);
    updateLanguageMock.mockResolvedValue(makeProfile({ languageCode: 'en', displayCurrency: 'USD' }));

    const { user } = renderWithProviders(
      <ToastProvider>
        <LocaleCurrencyStepSheet open onClose={() => {}} onContinue={() => {}} />
      </ToastProvider>,
    );

    await user.click(screen.getByText('Language'));
    await user.click(screen.getByText('English'));

    await waitFor(() => {
      expect(updateLanguageMock).toHaveBeenCalled();
      expect(updateLanguageMock.mock.calls[0][0]).toBe('en');
    });
  });
});

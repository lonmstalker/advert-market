import { ToastProvider } from '@telegram-tools/ui-kit';
import i18n from 'i18next';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { updateLanguage, updateSettings } from '@/shared/api/profile';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { renderWithProviders, screen } from '@/test/test-utils';
import { LocaleCurrencyEditor } from './locale-currency-editor';

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

function renderEditor() {
  return renderWithProviders(
    <ToastProvider>
      <LocaleCurrencyEditor mode="onboarding" />
    </ToastProvider>,
  );
}

describe('LocaleCurrencyEditor onboarding back behavior', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('en');
    vi.mocked(updateLanguage).mockReset();
    vi.mocked(updateLanguage).mockResolvedValue(makeProfile({ languageCode: 'ru', displayCurrency: 'RUB' }));
    vi.mocked(updateSettings).mockReset();
    vi.mocked(updateSettings).mockResolvedValue(makeProfile({ displayCurrency: 'EUR', currencyMode: 'MANUAL' }));

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

  it('shows Back button in onboarding language view', async () => {
    const { user } = renderEditor();
    await user.click(screen.getByText('Language'));
    expect(screen.getByText('Back')).toBeInTheDocument();
  });

  it('shows Back button in onboarding currency view', async () => {
    const { user } = renderEditor();
    await user.click(screen.getByText('Display Currency'));
    expect(screen.getByText('Back')).toBeInTheDocument();
  });
});

import { ToastProvider } from '@telegram-tools/ui-kit';
import i18n from 'i18next';
import { describe, expect, it, vi } from 'vitest';
import type { NotificationSettings, UserProfile } from '@/shared/api/auth';
import { updateLanguage } from '@/shared/api/profile';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { OnboardingSettingsSheet } from './onboarding-settings-sheet';

vi.mock('@/shared/api/profile', () => ({
  updateLanguage: vi.fn(),
}));

const DEFAULT_NOTIFICATIONS: NotificationSettings = {
  deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
  financial: { deposits: true, payouts: true, escrow: true },
  disputes: { opened: true, resolved: true },
};

describe('OnboardingSettingsSheet', () => {
  it('persists selected language via updateLanguage mutation', async () => {
    const updateLanguageMock = vi.mocked(updateLanguage);
    const isRu = i18n.language.toLowerCase().startsWith('ru');
    const targetCode = isRu ? 'en' : 'ru';
    const targetLabel = targetCode === 'ru' ? 'Русский' : 'English';

    const profile: UserProfile = {
      id: 1,
      telegramId: 1,
      username: 'user',
      displayName: 'User',
      languageCode: targetCode,
      displayCurrency: 'USD',
      notificationSettings: DEFAULT_NOTIFICATIONS,
      onboardingCompleted: false,
      interests: [],
      createdAt: '2026-02-15T00:00:00.000Z',
    };

    updateLanguageMock.mockResolvedValue(profile);

    const { user } = renderWithProviders(
      <ToastProvider>
        <OnboardingSettingsSheet open onClose={() => {}} />
      </ToastProvider>,
    );

    await expect(screen.findByText('Language')).resolves.toBeTruthy();

    const languageItem = screen.getByText(targetLabel).closest('[data-group-item="true"]');
    expect(languageItem).not.toBeNull();
    await user.click(languageItem as HTMLElement);

    await waitFor(() => {
      expect(updateLanguageMock).toHaveBeenCalledTimes(1);
      expect(updateLanguageMock.mock.calls[0][0]).toBe(targetCode);
    });
  });
});

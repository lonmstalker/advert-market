import type { Page } from '@playwright/test';

const E2E_ACCESS_TOKEN = 'mock-jwt-token-for-development';
const E2E_PROFILE_STATE = {
  id: 1,
  telegramId: 123456789,
  username: 'testuser',
  displayName: 'Test User',
  languageCode: 'ru',
  displayCurrency: 'USD',
  notificationSettings: {
    deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
    financial: { deposits: true, payouts: true, escrow: true },
    disputes: { opened: true, resolved: true },
  },
  onboardingCompleted: true,
  interests: ['advertiser'],
  createdAt: '2026-01-15T10:00:00Z',
};

export async function completeOnboarding(page: Page) {
  await page.goto('/');
  await page.getByRole('button', { name: 'Get Started' }).click();
  await page.getByRole('button', { name: /advertiser/i }).click();
  await page.getByRole('button', { name: 'Continue' }).click();
  await page.getByText('Skip').click();
  await page.waitForURL('**/catalog');
}

export async function seedOnboardedSession(page: Page) {
  await page.addInitScript(
    ({ accessToken, profile }) => {
      sessionStorage.setItem('access_token', accessToken);
      sessionStorage.setItem('msw_profile_state', JSON.stringify(profile));
    },
    { accessToken: E2E_ACCESS_TOKEN, profile: E2E_PROFILE_STATE },
  );
}

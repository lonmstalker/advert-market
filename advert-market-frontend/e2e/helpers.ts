import type { Page } from '@playwright/test';

const E2E_ACCESS_TOKEN = 'mock-jwt-token-for-development';
const E2E_PROFILE_STATE = {
  id: 1,
  telegramId: 123456789,
  username: 'testuser',
  displayName: 'Test User',
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
  createdAt: '2026-01-15T10:00:00Z',
};

export async function completeOnboarding(page: Page) {
  await page.goto('/onboarding', { waitUntil: 'domcontentloaded' });
  await page.getByTestId('onboarding-locale-step').waitFor({ state: 'visible' });

  // Locale-first UI: try selecting English in the visible picker when available.
  const languageRow = page.getByText(/^(Language|Язык)$/).first();
  if (await languageRow.isVisible({ timeout: 2_000 }).catch(() => false)) {
    await languageRow.click();
    const englishOption = page.getByText('English', { exact: true });
    if (await englishOption.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await englishOption.click();
    }
  }
  await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();

  await page.getByTestId('onboarding-welcome-step').waitFor({ state: 'visible' });
  await page.getByRole('button', { name: /^(Get Started|Начать)$/ }).click();

  await page.getByTestId('onboarding-interest-step').waitFor({ state: 'visible' });
  await page.getByRole('button', { name: /(advertiser|рекламодатель)/i }).click();
  await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();

  await page.getByTestId('onboarding-tour-step').waitFor({ state: 'visible' });
  await page.getByRole('button', { name: /(skip tutorial|пропустить обучение)/i }).click();
  const dialog = page.locator('[class*="dialogModalActive"]').first();
  await dialog.waitFor({ state: 'visible' });
  // DialogModal's footer actions are <div> wrappers (not <button>), so prefer structural locators over roles.
  await dialog.locator('[class*="dialogModalContentFooterButton"]').nth(1).click();

  await page.waitForURL('**/catalog');
}

export async function seedOnboardedSession(page: Page) {
  const payload = { accessToken: E2E_ACCESS_TOKEN, profile: E2E_PROFILE_STATE };

  await page.addInitScript(
    ({ accessToken, profile }) => {
      sessionStorage.setItem('access_token', accessToken);
      sessionStorage.setItem('msw_profile_state', JSON.stringify(profile));
    },
    payload,
  );

  // sessionStorage is unavailable on about:blank; ensure same-origin document first.
  if (!page.url().startsWith('http://') && !page.url().startsWith('https://')) {
    await page.goto('/', { waitUntil: 'domcontentloaded' });
  }

  await page.evaluate(
    ({ accessToken, profile }) => {
      sessionStorage.setItem('access_token', accessToken);
      sessionStorage.setItem('msw_profile_state', JSON.stringify(profile));
    },
    payload,
  );
}

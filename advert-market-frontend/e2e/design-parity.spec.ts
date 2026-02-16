import { expect, test } from '@playwright/test';

async function finishOnboarding(page: import('@playwright/test').Page) {
  await page.getByRole('button', { name: /^(Get Started|Начать)$/ }).click();
  await page.getByRole('button', { name: /(advertiser|рекламодатель)/i }).click();
  await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();
  await page.getByRole('button', { name: /(skip tutorial|пропустить обучение)/i }).click();

  const dialog = page.locator('[class*="dialogModalActive"]').first();
  await dialog.waitFor({ state: 'visible' });
  await dialog.locator('[class*="dialogModalContentFooterButton"]').nth(1).click();
  await page.waitForURL('**/catalog');
}

test.describe('Design parity visual baseline', () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test('captures telegram-native baseline screens', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Visual baseline is managed in chromium only.');
    const forcedTheme = (process.env.VITE_FORCE_THEME ?? '').toLowerCase();
    test.skip(!['light', 'dark'].includes(forcedTheme), 'Set VITE_FORCE_THEME=light|dark for visual parity.');

    const theme = forcedTheme;
    const screenshotOptions = {
      fullPage: true,
      animations: 'disabled' as const,
      maxDiffPixelRatio: 0.02,
    };

    await page.goto('/onboarding', { waitUntil: 'domcontentloaded' });
    await expect(page.getByTestId('onboarding-locale-step')).toBeVisible();
    await expect(page.getByTestId('onboarding-locale-logo')).toBeVisible();
    await expect(page).toHaveScreenshot(`${theme}-onboarding-locale.png`, screenshotOptions);

    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();
    await expect(page.getByTestId('onboarding-welcome-step')).toBeVisible();
    await expect(page).toHaveScreenshot(`${theme}-onboarding-welcome.png`, screenshotOptions);

    await finishOnboarding(page);

    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });
    await expect(page.getByTestId('catalog-page-shell')).toBeVisible();
    await expect(page.getByTestId('catalog-channel-card').first()).toBeVisible();
    await page.waitForTimeout(150);
    await expect(page).toHaveScreenshot(`${theme}-catalog-list.png`, screenshotOptions);

    await page.goto('/deals', { waitUntil: 'domcontentloaded' });
    await expect(page.getByTestId('deals-page-shell')).toBeVisible();
    await expect(page.getByTestId(/deal-list-item-/).first()).toBeVisible();
    await page.waitForTimeout(150);
    await expect(page).toHaveScreenshot(`${theme}-deals-list.png`, screenshotOptions);

    await page.goto('/wallet', { waitUntil: 'domcontentloaded' });
    await expect(page.getByTestId('wallet-page-shell')).toBeVisible();
    await expect(page).toHaveScreenshot(`${theme}-wallet-main.png`, screenshotOptions);

    await page.goto('/wallet/history', { waitUntil: 'domcontentloaded' });
    await expect(page.getByTestId('wallet-history-page-shell')).toBeVisible();
    await expect(page).toHaveScreenshot(`${theme}-wallet-history.png`, screenshotOptions);

    await page.goto('/wallet/history/tx-1', { waitUntil: 'domcontentloaded' });
    await expect(page.getByTestId('wallet-transaction-page-shell')).toBeVisible();
    await expect(page).toHaveScreenshot(`${theme}-wallet-transaction.png`, screenshotOptions);

    await page.goto('/profile', { waitUntil: 'domcontentloaded' });
    await expect(page.getByTestId('profile-page-shell')).toBeVisible();
    await expect(page).toHaveScreenshot(`${theme}-profile-main.png`, screenshotOptions);

    await page.goto('/profile/locale-currency', { waitUntil: 'domcontentloaded' });
    await expect(page.getByTestId('profile-locale-currency-page')).toBeVisible();
    await expect(page).toHaveScreenshot(`${theme}-profile-locale-currency.png`, screenshotOptions);
  });
});

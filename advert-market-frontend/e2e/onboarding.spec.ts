import { expect, test } from '@playwright/test';

test.describe('Onboarding Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/onboarding', { waitUntil: 'domcontentloaded' });
  });

  test('shows locale-first step with service logo', async ({ page }) => {
    await expect(page.getByTestId('onboarding-locale-step')).toBeVisible();
    await expect(page.getByTestId('onboarding-locale-logo')).toBeVisible();
    await expect(page.getByRole('button', { name: /^(Continue|Продолжить)$/ })).toBeVisible();
  });

  test('applies selected language before welcome and tutorial', async ({ page }) => {
    await page.getByText('Language', { exact: true }).click();
    await page.getByText('Русский', { exact: true }).click();
    await page.getByRole('button', { name: 'Продолжить' }).click();

    await expect(page.getByText('Маркетплейс рекламы в Telegram-каналах')).toBeVisible();
    await page.getByRole('button', { name: 'Начать' }).click();
    await expect(page.getByText('Кто вы?')).toBeVisible();

    await page.getByRole('button', { name: /рекламодатель/i }).click();
    await page.getByRole('button', { name: 'Продолжить' }).click();
    await expect(page.getByRole('button', { name: 'Далее' })).toBeVisible();
  });

  test('completes full onboarding as advertiser', async ({ page }) => {
    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();
    await page.getByRole('button', { name: /^(Get Started|Начать)$/ }).click();

    await expect(page.getByText(/^(Who are you\?|Кто вы\?)$/)).toBeVisible();
    const continueBtn = page.getByRole('button', { name: /^(Continue|Продолжить)$/ });
    await expect(continueBtn).toBeDisabled();

    await page.getByRole('button', { name: /(advertiser|рекламодатель)/i }).click();
    await expect(continueBtn).toBeEnabled();
    await continueBtn.click();

    await expect(page.getByText(/^(Find Channels|Найдите каналы)$/)).toBeVisible();
    await page.getByRole('button', { name: /^(Next|Далее)$/ }).click();
    await expect(page.getByText(/^(Secure Deals|Безопасные сделки)$/)).toBeVisible();
    await page.getByRole('button', { name: /^(Next|Далее)$/ }).click();
    await expect(page.getByText(/^(Secure Payments|Безопасные платежи)$/)).toBeVisible();

    await page.getByRole('button', { name: /^(Open catalog|Открыть каталог)$/ }).click();
    await page.waitForURL('**/catalog');
  });

  test('completes onboarding as owner and lands on add channel', async ({ page }) => {
    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();
    await page.getByRole('button', { name: /^(Get Started|Начать)$/ }).click();
    await page.getByRole('button', { name: /(channel owner|владелец канала)/i }).click();
    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();

    await page.getByRole('button', { name: /(skip tutorial|пропустить обучение)/i }).click();
    const dialog = page.locator('[class*="dialogModalActive"]').first();
    await expect(dialog).toBeVisible();
    await dialog.locator('[class*="dialogModalContentFooterButton"]').nth(1).click();

    await page.waitForURL('**/profile/channels/new');
  });

  test('supports skip flow from tutorial', async ({ page }) => {
    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();
    await page.getByRole('button', { name: /^(Get Started|Начать)$/ }).click();
    await page.getByRole('button', { name: /(advertiser|рекламодатель)/i }).click();
    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();

    await page.getByRole('button', { name: /(skip tutorial|пропустить обучение)/i }).click();
    const dialog = page.locator('[class*="dialogModalActive"]').first();
    await expect(dialog).toBeVisible();
    await dialog.locator('[class*="dialogModalContentFooterButton"]').nth(1).click();

    await page.waitForURL('**/catalog');
  });

  test('redirects to interest page when navigating directly to tour', async ({ page }) => {
    try {
      await page.goto('/onboarding/tour', { waitUntil: 'domcontentloaded' });
    } catch {
      // WebKit can abort `page.goto` during fast SPA redirects.
    }

    await expect(page.getByText(/^(Who are you\?|Кто вы\?)$/)).toBeVisible();
  });
});

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

  test('completes onboarding as owner and lands on catalog', async ({ page }) => {
    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();
    await page.getByRole('button', { name: /^(Get Started|Начать)$/ }).click();
    await page.getByRole('button', { name: /(channel owner|владелец канала)/i }).click();
    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();

    await page.getByRole('button', { name: /(skip tutorial|пропустить обучение)/i }).click();
    const dialog = page.locator('[class*="dialogModalActive"]').first();
    await expect(dialog).toBeVisible();
    await dialog.locator('[class*="dialogModalContentFooterButton"]').nth(1).click();

    await page.waitForURL('**/catalog');
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

  test('with both roles selected, flow lands on catalog', async ({ page }) => {
    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();
    await page.getByRole('button', { name: /^(Get Started|Начать)$/ }).click();

    await page.getByRole('button', { name: /(advertiser|рекламодатель)/i }).click();
    await page.getByRole('button', { name: /(channel owner|владелец канала)/i }).click();
    await expect(
      page.getByText(
        /(We'll start from Catalog\. Owner tools stay available in Profile\.|Стартуем с каталога\. Инструменты владельца доступны в профиле\.)/,
      ),
    ).toBeVisible();

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

test.describe('Onboarding desktop layout', () => {
  test.use({ viewport: { width: 1440, height: 900 } });

  test('uses full-width onboarding shell with safe gutters on desktop', async ({ page }) => {
    await page.goto('/onboarding', { waitUntil: 'domcontentloaded' });
    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();
    await expect(page.getByText(/^(Channel Catalog|Каталог каналов)$/)).toBeVisible();

    const featureStack = page.locator('.am-onboarding-feature-stack').first();
    await expect(featureStack).toBeVisible();
    const cardBox = await featureStack.boundingBox();
    expect(cardBox).not.toBeNull();
    const shellBox = await page.locator('.am-onboarding-shell__container').boundingBox();
    expect(shellBox).not.toBeNull();

    const viewport = page.viewportSize();
    expect(viewport).not.toBeNull();

    const cardLeft = cardBox!.x;
    const cardRight = viewport!.width - (cardBox!.x + cardBox!.width);

    expect(shellBox!.width).toBeGreaterThanOrEqual(viewport!.width * 0.9);
    expect(cardBox!.width).toBeGreaterThanOrEqual(viewport!.width * 0.9);
    expect(cardLeft).toBeGreaterThanOrEqual(12);
    expect(cardRight).toBeGreaterThanOrEqual(12);
    expect(Math.abs(cardLeft - cardRight)).toBeLessThanOrEqual(3);
  });

  test('keeps role cards visually prominent on desktop', async ({ page }) => {
    await page.goto('/onboarding', { waitUntil: 'domcontentloaded' });
    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();
    await page.getByRole('button', { name: /^(Get Started|Начать)$/ }).click();
    await expect(page.getByText(/^(Who are you\?|Кто вы\?)$/)).toBeVisible();

    const roleCard = page.getByTestId('role-card-trigger').first();
    await expect(roleCard).toBeVisible();
    const roleBox = await roleCard.boundingBox();
    expect(roleBox).not.toBeNull();
    expect(roleBox!.height).toBeGreaterThanOrEqual(107);
  });

  test('uses neutral UI Kit surfaces (no onboarding gradients)', async ({ page }) => {
    await page.goto('/onboarding', { waitUntil: 'domcontentloaded' });
    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();
    await expect(page.getByText(/^(Channel Catalog|Каталог каналов)$/)).toBeVisible();

    const shellBackgroundImage = await page.locator('.am-onboarding-shell').first().evaluate((el) => {
      return getComputedStyle(el).backgroundImage;
    });
    expect(shellBackgroundImage).toBe('none');

    const stackBackgroundImage = await page.locator('.am-onboarding-feature-stack').first().evaluate((el) => {
      return getComputedStyle(el).backgroundImage;
    });
    expect(stackBackgroundImage).toBe('none');
  });

  test('keeps onboarding CTA inside viewport width', async ({ page }) => {
    await page.goto('/onboarding', { waitUntil: 'domcontentloaded' });
    await page.getByRole('button', { name: /^(Continue|Продолжить)$/ }).click();
    await page.getByRole('button', { name: /^(Get Started|Начать)$/ }).click();
    await expect(page.getByText(/^(Who are you\?|Кто вы\?)$/)).toBeVisible();

    const cta = page.getByRole('button', { name: /^(Continue|Продолжить)$/ });
    await expect(cta).toBeVisible();
    const box = await cta.boundingBox();
    expect(box).not.toBeNull();

    const viewport = page.viewportSize();
    expect(viewport).not.toBeNull();
    const right = box!.x + box!.width;
    const leftGutter = box!.x;
    const rightGutter = viewport!.width - right;

    expect(leftGutter).toBeGreaterThanOrEqual(12);
    expect(rightGutter).toBeGreaterThanOrEqual(12);
  });
});

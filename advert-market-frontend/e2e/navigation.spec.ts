import { expect, test } from '@playwright/test';
import { seedOnboardedSession } from './helpers';

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await seedOnboardedSession(page);
  });

  test('bottom tabs show all 4 tabs', async ({ page }) => {
    await page.goto('/catalog', { waitUntil: 'commit' });
    await expect(page.getByRole('link', { name: /^(Catalog|Каталог)$/ })).toBeVisible();
    await expect(page.getByRole('link', { name: /^(Deals|Сделки)$/ })).toBeVisible();
    await expect(page.getByRole('link', { name: /^(Finance|Финансы)$/ })).toBeVisible();
    await expect(page.getByRole('link', { name: /^(Profile|Профиль)$/ })).toBeVisible();
  });

  test('clicking Deals tab navigates to deals page', async ({ page }) => {
    await page.goto('/catalog', { waitUntil: 'commit' });
    await page.getByRole('link', { name: /^(Deals|Сделки)$/ }).click();
    await page.waitForURL('**/deals');
    await expect(page.getByText(/^(Deals|Сделки)$/).first()).toBeVisible();
  });

  test('clicking Wallet tab navigates to wallet page', async ({ page }) => {
    await page.goto('/catalog', { waitUntil: 'commit' });
    await page.getByRole('link', { name: /^(Finance|Финансы)$/ }).click();
    await page.waitForURL('**/wallet');
  });

  test('clicking Profile tab navigates to profile page', async ({ page }) => {
    await page.goto('/catalog', { waitUntil: 'commit' });
    await page.getByRole('link', { name: /^(Profile|Профиль)$/ }).click();
    await expect(page.getByText(/^(Settings|Настройки)$/)).toBeVisible();
  });

  test('clicking Catalog tab returns to catalog', async ({ page }) => {
    await page.goto('/catalog', { waitUntil: 'commit' });
    await page.getByRole('link', { name: /^(Deals|Сделки)$/ }).click();
    await page.waitForURL('**/deals');
    await page.getByRole('link', { name: /^(Catalog|Каталог)$/ }).click();
    await page.waitForURL('**/catalog');
  });

  test('deep link to deals page works after onboarding', async ({ page }) => {
    // WebKit can hang waiting for the full "load" event on SPA deep links in dev mode.
    await page.goto('/deals', { waitUntil: 'commit' });
    await expect(page.getByText(/^(Deals|Сделки)$/).first()).toBeVisible();
  });

  test('deep link to channel detail works', async ({ page }) => {
    await page.goto('/catalog/channels/1', { waitUntil: 'commit' });
    await expect(page.getByText('Crypto News Daily')).toBeVisible();
  });

  test('browser back button works', async ({ page }) => {
    await page.goto('/catalog', { waitUntil: 'commit' });
    await page.getByRole('link', { name: /^(Deals|Сделки)$/ }).click();
    await page.waitForURL('**/deals');
    await page.goBack();
    await page.waitForURL('**/catalog');
  });
});

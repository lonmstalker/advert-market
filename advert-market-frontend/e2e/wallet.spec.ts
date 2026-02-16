import { expect, test } from '@playwright/test';
import { completeOnboarding } from './helpers';

test.describe('Wallet Page', () => {
  test.beforeEach(async ({ page }) => {
    await completeOnboarding(page);
    await page.getByRole('link', { name: /^(Finance|Финансы)$/ }).click();
    await page.waitForURL('**/wallet');
  });

  test('displays wallet page', async ({ page }) => {
    await expect(page.getByTestId('wallet-page-shell')).toBeVisible();
    await expect(page.getByText(/^(Crypto|Крипто)$/i)).toBeVisible();
  });

  test('bottom tabs remain visible', async ({ page }) => {
    await expect(page.getByRole('link', { name: /^(Catalog|Каталог)$/ })).toBeVisible();
  });

  test('can navigate back to catalog', async ({ page }) => {
    await page.getByRole('link', { name: /^(Catalog|Каталог)$/ }).click();
    await page.waitForURL('**/catalog');
  });
});

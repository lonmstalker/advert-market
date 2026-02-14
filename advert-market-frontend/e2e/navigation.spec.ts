import { expect, test } from '@playwright/test';
import { completeOnboarding } from './helpers';

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await completeOnboarding(page);
  });

  test('bottom tabs show all 4 tabs', async ({ page }) => {
    await expect(page.getByRole('link', { name: 'Catalog' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Deals' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Wallet' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Profile' })).toBeVisible();
  });

  test('clicking Deals tab navigates to deals page', async ({ page }) => {
    await page.getByRole('link', { name: 'Deals' }).click();
    await page.waitForURL('**/deals');
    await expect(page.getByText('Deals').first()).toBeVisible();
  });

  test('clicking Wallet tab navigates to wallet page', async ({ page }) => {
    await page.getByRole('link', { name: 'Wallet' }).click();
    await page.waitForURL('**/wallet');
  });

  test('clicking Profile tab navigates to profile page', async ({ page }) => {
    await page.getByRole('link', { name: 'Profile' }).click();
    await page.waitForURL('**/profile');
  });

  test('clicking Catalog tab returns to catalog', async ({ page }) => {
    await page.getByRole('link', { name: 'Deals' }).click();
    await page.waitForURL('**/deals');
    await page.getByRole('link', { name: 'Catalog' }).click();
    await page.waitForURL('**/catalog');
  });

  test('deep link to deals page works after onboarding', async ({ page }) => {
    await page.goto('/deals');
    await expect(page.getByText('Deals').first()).toBeVisible();
  });

  test('deep link to channel detail works', async ({ page }) => {
    await page.goto('/catalog/channels/1', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Crypto News Daily')).toBeVisible();
  });

  test('browser back button works', async ({ page }) => {
    await page.getByRole('link', { name: 'Deals' }).click();
    await page.waitForURL('**/deals');
    await page.goBack();
    await page.waitForURL('**/catalog');
  });
});

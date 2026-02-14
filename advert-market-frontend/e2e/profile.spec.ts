import { expect, test } from '@playwright/test';
import { completeOnboarding } from './helpers';

test.describe('Profile Page', () => {
  test.beforeEach(async ({ page }) => {
    await completeOnboarding(page);
    await page.getByRole('link', { name: 'Profile' }).click();
    await expect(page.getByText(/^Settings$/)).toBeVisible();
  });

  test('displays profile page', async ({ page }) => {
    await expect(page.getByText(/^Settings$/)).toBeVisible();
  });

  test('bottom tabs remain visible', async ({ page }) => {
    await expect(page.getByRole('link', { name: 'Catalog' })).toBeVisible();
  });

  test('can navigate back to catalog', async ({ page }) => {
    await page.getByRole('link', { name: 'Catalog' }).click();
    await page.waitForURL('**/catalog');
  });
});

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

  test('can open add channel page', async ({ page }) => {
    await page.getByRole('button', { name: 'Add channel' }).click();
    await page.waitForURL('**/profile/channels/new');
    await expect(page.getByText('Add Channel')).toBeVisible();
  });
});

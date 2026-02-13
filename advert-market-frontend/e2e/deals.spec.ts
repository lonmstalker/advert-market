import { expect, type Page, test } from '@playwright/test';
import { completeOnboarding } from './helpers';

async function navigateToDeals(page: Page) {
  await completeOnboarding(page);
  await page.getByRole('link', { name: 'Deals' }).click();
  await page.waitForURL('**/deals');
}

test.describe('Deals Page', () => {
  test('displays deal list with segment control', async ({ page }) => {
    await navigateToDeals(page);
    await expect(page.getByText('Deals').first()).toBeVisible();
    await expect(page.getByRole('button', { name: 'Advertiser' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Owner' })).toBeVisible();
  });

  test('shows advertiser deals by default', async ({ page }) => {
    await navigateToDeals(page);
    // deal-1: OFFER_PENDING, Tech Digest, role=ADVERTISER
    await expect(page.getByText('Tech Digest').first()).toBeVisible();
  });

  test('switching to Owner tab shows owner deals', async ({ page }) => {
    await navigateToDeals(page);
    await page.getByRole('button', { name: 'Owner' }).click();
    // deal-4: FUNDED, Crypto News Daily, role=OWNER
    await expect(page.getByText('Crypto News Daily').first()).toBeVisible();
  });

  test('clicking a deal navigates to detail page', async ({ page }) => {
    await navigateToDeals(page);
    await expect(page.getByText('Tech Digest').first()).toBeVisible();
    await page.getByText('Tech Digest').first().click();
    await page.waitForURL('**/deals/deal-1');
    await expect(page.getByText('Timeline')).toBeVisible();
  });

  test('deal detail shows timeline and actions', async ({ page }) => {
    await navigateToDeals(page);
    await page.getByText('Tech Digest').first().click();
    await page.waitForURL('**/deals/deal-1');
    // Should show status badge
    await expect(page.getByText('Offer Pending')).toBeVisible();
    // Should show timeline
    await expect(page.getByText('Timeline')).toBeVisible();
    // Advertiser on OFFER_PENDING should see Cancel button
    await expect(page.getByRole('button', { name: 'Cancel' })).toBeVisible();
  });
});

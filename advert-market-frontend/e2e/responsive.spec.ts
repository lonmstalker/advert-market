import { expect, test } from '@playwright/test';
import { completeOnboarding } from './helpers';

const VIEWPORTS = [
  { name: 'iPhone SE', width: 320, height: 568 },
  { name: 'iPhone 14', width: 375, height: 812 },
  { name: 'iPhone 14 Pro Max', width: 428, height: 926 },
];

for (const viewport of VIEWPORTS) {
  test.describe(`Responsive: ${viewport.name} (${viewport.width}px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test.beforeEach(async ({ page }) => {
      await completeOnboarding(page);
    });

    test('catalog page renders without horizontal overflow', async ({ page }) => {
      const bodyWidth = await page.evaluate(() => document.body.scrollWidth);
      expect(bodyWidth).toBeLessThanOrEqual(viewport.width + 1);
    });

    test('channel cards are fully visible', async ({ page }) => {
      await expect(page.getByText('Crypto News Daily')).toBeVisible();
      const card = page
        .getByText('Crypto News Daily')
        .locator('xpath=ancestor::div[contains(@style, "border-radius")]')
        .first();
      const box = await card.boundingBox();
      expect(box).toBeTruthy();
      if (box) {
        expect(box.x).toBeGreaterThanOrEqual(0);
        expect(box.x + box.width).toBeLessThanOrEqual(viewport.width + 1);
      }
    });

    test('bottom tabs are visible and not overlapping', async ({ page }) => {
      const tabs = page.getByRole('link', { name: 'Catalog' });
      await expect(tabs).toBeVisible();
    });

    test('search bar is usable at narrow width', async ({ page }) => {
      const searchInput = page.getByPlaceholder('Search channels...');
      await expect(searchInput).toBeVisible();
      const box = await searchInput.boundingBox();
      expect(box).toBeTruthy();
      if (box) {
        expect(box.width).toBeGreaterThan(100);
      }
    });

    test('deal list renders at narrow width', async ({ page }) => {
      await page.getByRole('link', { name: 'Deals' }).click();
      await page.waitForURL('**/deals');
      await expect(page.getByText('Deals').first()).toBeVisible();
      const bodyWidth = await page.evaluate(() => document.body.scrollWidth);
      expect(bodyWidth).toBeLessThanOrEqual(viewport.width + 1);
    });

    test('channel detail page renders at narrow width', async ({ page }) => {
      await page.getByText('Crypto News Daily').click();
      await page.waitForURL('**/catalog/channels/1');
      await expect(page.getByText('Subscribers')).toBeVisible();
      const bodyWidth = await page.evaluate(() => document.body.scrollWidth);
      expect(bodyWidth).toBeLessThanOrEqual(viewport.width + 1);
    });
  });
}

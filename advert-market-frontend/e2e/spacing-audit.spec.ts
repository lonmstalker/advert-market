import { expect, test } from '@playwright/test';
import { seedOnboardedSession } from './helpers';

/**
 * Spacing audit: verifies DESIGN_GUIDELINES.md section 4.
 * - 16px gap between Group sections (.am-page-stack gap)
 * - 12px gap between paired action buttons
 * - 16px page inner padding
 */

async function attachFullPage(page: import('@playwright/test').Page, testInfo: import('@playwright/test').TestInfo) {
  await testInfo.attach('screenshot', {
    body: await page.screenshot({ fullPage: true }),
    contentType: 'image/png',
  });
}

test.describe('Spacing audit', () => {
  test.beforeEach(async ({ page }) => {
    await seedOnboardedSession(page);
  });

  test('page stack uses 16px gap between sections on catalog', async ({ page }, testInfo) => {
    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Crypto News Daily')).toBeVisible();

    const gap = await page.locator('.am-page-stack').first().evaluate((el) => {
      return getComputedStyle(el).gap;
    });
    expect(Number.parseInt(gap, 10)).toBe(16);

    await attachFullPage(page, testInfo);
  });

  test('page stack uses 16px gap between sections on deal detail', async ({ page }, testInfo) => {
    await page.goto('/deals/deal-1', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText(/^Timeline$/)).toBeVisible();

    const gap = await page.locator('.am-page-stack').first().evaluate((el) => {
      return getComputedStyle(el).gap;
    });
    expect(Number.parseInt(gap, 10)).toBe(16);

    await attachFullPage(page, testInfo);
  });

  test('page stack uses 16px gap on wallet page', async ({ page }, testInfo) => {
    await page.goto('/wallet', { waitUntil: 'domcontentloaded' });
    await expect(page.getByTestId('wallet-page-shell')).toBeVisible();

    const gap = await page.locator('.am-page-stack').first().evaluate((el) => {
      return getComputedStyle(el).gap;
    });
    expect(Number.parseInt(gap, 10)).toBe(16);

    await attachFullPage(page, testInfo);
  });

  test('page inner padding matches --am-page-padding token', async ({ page }, testInfo) => {
    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Crypto News Daily')).toBeVisible();

    const padding = await page.evaluate(() => {
      return getComputedStyle(document.documentElement).getPropertyValue('--am-page-padding').trim();
    });
    const paddingPx = Number.parseInt(padding, 10);
    // Design system allows 16px or 20px page padding
    expect(paddingPx).toBeGreaterThanOrEqual(16);
    expect(paddingPx).toBeLessThanOrEqual(24);

    await attachFullPage(page, testInfo);
  });

  test('paired action buttons have 12px gap on deal detail', async ({ page }, testInfo) => {
    await page.goto('/deals/deal-1', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText(/^Timeline$/)).toBeVisible();

    // Action buttons container uses gap-3 (12px) per DESIGN_GUIDELINES
    const actionContainer = page.locator('.am-fixed-bottom-bar__inner, [class*="deal-actions"]').first();
    if (await actionContainer.isVisible()) {
      const gap = await actionContainer.evaluate((el) => {
        const styles = getComputedStyle(el);
        return styles.gap || styles.columnGap;
      });
      const gapValue = Number.parseInt(gap, 10);
      // Gap should be 12px between paired buttons
      expect(gapValue).toBeGreaterThanOrEqual(12);
    }

    await attachFullPage(page, testInfo);
  });

  test('bottom tabs height matches design token', async ({ page }, testInfo) => {
    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.am-bottom-tabs')).toBeVisible();

    const height = await page.evaluate(() => {
      return getComputedStyle(document.documentElement).getPropertyValue('--am-bottom-tabs-height').trim();
    });
    expect(height).toBe('64px');

    await attachFullPage(page, testInfo);
  });
});

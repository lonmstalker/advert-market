import { expect, test } from '@playwright/test';
import { seedOnboardedSession } from './helpers';

/**
 * Typography audit: enforces max 3 distinct font-size levels per viewport
 * (DESIGN_GUIDELINES.md section 6 — Three-Level Rule).
 *
 * Sizes within 3px of each other count as a single visual level
 * (e.g., 26px title2 and 28px title1 are visually close enough to be one level).
 */

async function collectFontSizes(page: import('@playwright/test').Page, selector: string): Promise<number[]> {
  return page.evaluate((sel) => {
    const elements = document.querySelectorAll(sel);
    const sizes = new Set<number>();
    for (const el of elements) {
      const styles = getComputedStyle(el);
      if (styles.display === 'none' || styles.visibility === 'hidden' || styles.opacity === '0') continue;
      const rect = el.getBoundingClientRect();
      if (rect.width === 0 || rect.height === 0) continue;
      sizes.add(Math.round(Number.parseFloat(styles.fontSize)));
    }
    return [...sizes].sort((a, b) => b - a);
  }, selector);
}

/**
 * Group font sizes into visual "levels" — sizes within 3px of each other
 * are considered the same visual level.
 */
function groupIntoLevels(sizes: number[]): number[][] {
  if (sizes.length === 0) return [];
  const sorted = [...sizes].sort((a, b) => b - a);
  const groups: number[][] = [[sorted[0]]];
  for (let i = 1; i < sorted.length; i++) {
    const lastGroup = groups[groups.length - 1];
    if (lastGroup[0] - sorted[i] <= 3) {
      lastGroup.push(sorted[i]);
    } else {
      groups.push([sorted[i]]);
    }
  }
  return groups;
}

async function attachFullPage(page: import('@playwright/test').Page, testInfo: import('@playwright/test').TestInfo) {
  await testInfo.attach('screenshot', {
    body: await page.screenshot({ fullPage: true }),
    contentType: 'image/png',
  });
}

test.describe('Typography audit — Three-Level Rule', () => {
  test.beforeEach(async ({ page }) => {
    await seedOnboardedSession(page);
  });

  test('catalog page uses at most 3 visual heading levels', async ({ page }, testInfo) => {
    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Crypto News Daily')).toBeVisible();

    const sizes = await collectFontSizes(page, '.am-page-stack *');
    await attachFullPage(page, testInfo);

    const headingSizes = sizes.filter((s) => s >= 17);
    const levels = groupIntoLevels(headingSizes);
    expect(
      levels.length,
      `Expected max 3 heading levels, got ${levels.length}: ${levels.map((g) => g.join('/')).join(', ')}px`,
    ).toBeLessThanOrEqual(3);
  });

  test('deal detail page uses at most 3 visual heading levels', async ({ page }, testInfo) => {
    await page.goto('/deals/deal-1', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText(/^Timeline$/)).toBeVisible();

    const sizes = await collectFontSizes(page, '.am-page-stack *');
    await attachFullPage(page, testInfo);

    const headingSizes = sizes.filter((s) => s >= 17);
    const levels = groupIntoLevels(headingSizes);
    expect(
      levels.length,
      `Expected max 3 heading levels, got ${levels.length}: ${levels.map((g) => g.join('/')).join(', ')}px`,
    ).toBeLessThanOrEqual(3);
  });

  test('wallet page uses at most 3 visual heading levels', async ({ page }, testInfo) => {
    await page.goto('/wallet', { waitUntil: 'domcontentloaded' });
    await expect(page.getByTestId('wallet-page-shell')).toBeVisible();

    const sizes = await collectFontSizes(page, '.am-page-stack *');
    await attachFullPage(page, testInfo);

    const headingSizes = sizes.filter((s) => s >= 17);
    const levels = groupIntoLevels(headingSizes);
    expect(
      levels.length,
      `Expected max 3 heading levels, got ${levels.length}: ${levels.map((g) => g.join('/')).join(', ')}px`,
    ).toBeLessThanOrEqual(3);
  });

  test('creative editor uses at most 3 visual heading levels', async ({ page }, testInfo) => {
    await page.goto('/profile/creatives/new', { waitUntil: 'domcontentloaded' });
    await expect(page.getByRole('button', { name: 'Save' })).toBeVisible();

    const sizes = await collectFontSizes(page, '.am-page-stack *');
    await attachFullPage(page, testInfo);

    const headingSizes = sizes.filter((s) => s >= 17);
    const levels = groupIntoLevels(headingSizes);
    expect(
      levels.length,
      `Expected max 3 heading levels, got ${levels.length}: ${levels.map((g) => g.join('/')).join(', ')}px`,
    ).toBeLessThanOrEqual(3);
  });
});

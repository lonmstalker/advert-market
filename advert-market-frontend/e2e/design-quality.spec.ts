import { expect, test } from '@playwright/test';
import { seedOnboardedSession } from './helpers';

type ParsedColor = { r: number; g: number; b: number; a: number };

function clamp255(n: number): number {
  return Math.max(0, Math.min(255, n));
}

function parseCssColor(color: string): ParsedColor | null {
  const v = color.trim().toLowerCase();
  if (v === 'transparent') return { r: 0, g: 0, b: 0, a: 0 };

  // rgba(1, 2, 3, 0.12)
  const rgba = v.match(/^rgba\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*([0-9.]+)\s*\)$/);
  if (rgba) return { r: Number(rgba[1]), g: Number(rgba[2]), b: Number(rgba[3]), a: Number(rgba[4]) };

  // rgb(1, 2, 3)
  const rgb = v.match(/^rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)$/);
  if (rgb) return { r: Number(rgb[1]), g: Number(rgb[2]), b: Number(rgb[3]), a: 1 };

  // rgb(1 2 3 / 0.12)
  const rgbSlash = v.match(/^rgb\(\s*(\d+)\s+(\d+)\s+(\d+)\s*\/\s*([0-9.]+)\s*\)$/);
  if (rgbSlash) return { r: Number(rgbSlash[1]), g: Number(rgbSlash[2]), b: Number(rgbSlash[3]), a: Number(rgbSlash[4]) };

  // rgb(1 2 3)
  const rgbSpace = v.match(/^rgb\(\s*(\d+)\s+(\d+)\s+(\d+)\s*\)$/);
  if (rgbSpace) return { r: Number(rgbSpace[1]), g: Number(rgbSpace[2]), b: Number(rgbSpace[3]), a: 1 };

  // color(display-p3 1 0 0 / 0.12) or color(srgb 0.1 0.2 0.3)
  const modern = v.match(/^color\(\s*(display-p3|srgb)\s+([0-9.]+)\s+([0-9.]+)\s+([0-9.]+)(?:\s*\/\s*([0-9.]+))?\s*\)$/);
  if (modern) {
    const r = clamp255(Math.round(Number(modern[2]) * 255));
    const g = clamp255(Math.round(Number(modern[3]) * 255));
    const b = clamp255(Math.round(Number(modern[4]) * 255));
    const a = modern[5] != null ? Number(modern[5]) : 1;
    return { r, g, b, a };
  }

  return null;
}

function colorDistance(a: ParsedColor, b: ParsedColor): number {
  return Math.abs(a.r - b.r) + Math.abs(a.g - b.g) + Math.abs(a.b - b.b) + Math.abs(a.a - b.a) * 255;
}

async function attachFullPage(page: import('@playwright/test').Page, testInfo: import('@playwright/test').TestInfo) {
  await testInfo.attach('screenshot', {
    body: await page.screenshot({ fullPage: true }),
    contentType: 'image/png',
  });
}

async function resolveCssVarColor(page: import('@playwright/test').Page, varName: string): Promise<ParsedColor> {
  const computed = await page.evaluate((name) => {
    const el = document.createElement('div');
    el.style.position = 'fixed';
    el.style.left = '-9999px';
    el.style.top = '-9999px';
    el.style.width = '1px';
    el.style.height = '1px';
    el.style.backgroundColor = `var(${name})`;
    document.body.appendChild(el);
    const value = getComputedStyle(el).backgroundColor;
    el.remove();
    return value;
  }, varName);

  const parsed = parseCssColor(computed);
  expect(parsed).not.toBeNull();
  return parsed as ParsedColor;
}

test.describe('Design quality (TG-native / iOS-like)', () => {
  test.beforeEach(async ({ page }) => {
    await seedOnboardedSession(page);
  });

  test('Soft badges: deal status backgrounds are tinted (semi-transparent)', async ({ page }, testInfo) => {
    await page.goto('/deals/deal-1', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText(/^Timeline$/)).toBeVisible();

    // DealStatusBadge uses an inline span with "padding: 2px 8px" and a soft background.
    const dealStatus = page.locator('span[style*="padding: 2px 8px"]').filter({ hasText: 'Offer Pending' }).first();
    await expect(dealStatus).toBeVisible();

    const dealBg = await dealStatus.evaluate((el) => getComputedStyle(el).backgroundColor);
    const dealColor = parseCssColor(dealBg);
    expect(dealColor).not.toBeNull();
    // Soft badge contract: subtle tint, not an opaque pill.
    expect((dealColor as ParsedColor).a).toBeGreaterThan(0.01);
    expect((dealColor as ParsedColor).a).toBeLessThanOrEqual(0.25);

    await attachFullPage(page, testInfo);
  });

  test('Soft badges: wallet status backgrounds are tinted (semi-transparent)', async ({ page }, testInfo) => {
    await page.goto('/wallet/history/tx-1', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('View in TON Explorer')).toBeVisible();

    const txStatus = page.locator('span[style*="padding: 2px 8px"]').filter({ hasText: 'Confirmed' }).first();
    await expect(txStatus).toBeVisible();

    const txBg = await txStatus.evaluate((el) => getComputedStyle(el).backgroundColor);
    const txColor = parseCssColor(txBg);
    expect(txColor).not.toBeNull();
    expect((txColor as ParsedColor).a).toBeGreaterThan(0.01);
    expect((txColor as ParsedColor).a).toBeLessThanOrEqual(0.25);

    await attachFullPage(page, testInfo);
  });

  test('S14: toggles follow accent color (not hardcoded green)', async ({ page }, testInfo) => {
    await page.goto('/profile/notifications', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Notifications')).toBeVisible();

    const enabledToggler = page.locator('[class*="_togglerEnabled_"][class*="_toggler_"]').first();
    await expect(enabledToggler).toBeVisible();

    const togglerBgRaw = await enabledToggler.evaluate((el) => getComputedStyle(el).backgroundColor);
    const togglerBg = parseCssColor(togglerBgRaw);
    expect(togglerBg).not.toBeNull();

    // Contract: enabled Toggle follows accent-primary (Telegram theme), not the UI Kit success-green.
    const accent = await resolveCssVarColor(page, '--color-accent-primary');
    const success = await resolveCssVarColor(page, '--color-state-success');

    const dAccent = colorDistance(togglerBg as ParsedColor, accent);
    const dSuccess = colorDistance(togglerBg as ParsedColor, success);

    expect(dAccent).toBeLessThanOrEqual(10);
    expect(dSuccess).toBeGreaterThanOrEqual(30);

    await attachFullPage(page, testInfo);
  });

  test('feature card icon box is 56px', async ({ page }, testInfo) => {
    await page.goto('/onboarding', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Channel Catalog')).toBeVisible();

    const iconBox = page.getByTestId('feature-icon-box').first();
    await expect(iconBox).toBeVisible();
    const box = await iconBox.boundingBox();
    expect(box).not.toBeNull();
    expect(box!.width).toBeCloseTo(56, 0);
    expect(box!.height).toBeCloseTo(56, 0);

    await attachFullPage(page, testInfo);
  });

  test('timeline items have minimum gap', async ({ page }, testInfo) => {
    await page.goto('/deals/deal-1', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText(/^Timeline$/)).toBeVisible();

    const timelineUl = page.locator('ul[aria-label]').first();
    await expect(timelineUl).toBeVisible();
    const gap = await timelineUl.evaluate((el) => getComputedStyle(el).gap);
    expect(Number.parseInt(gap, 10)).toBeGreaterThanOrEqual(4);

    await attachFullPage(page, testInfo);
  });

  test('creative preview has no DeviceFrame gradient border', async ({ page }, testInfo) => {
    await page.goto('/profile/creatives/new', { waitUntil: 'domcontentloaded' });
    await expect(page.getByRole('button', { name: 'Save' })).toBeVisible();

    // Mobile layout: switch to preview tab. Desktop layout renders preview side-by-side.
    const previewTab = page.getByRole('button', { name: /^Preview$/ });
    if (await previewTab.isVisible()) {
      await previewTab.click();
    }
    // Preview exists in both desktop (always visible) and mobile (tabbed) layouts; assert against the visible one only.
    const visiblePreview = page
      .locator('.creative-editor-mobile:visible, .creative-editor-desktop:visible')
      .filter({ has: page.locator('text=/subscribers/i') })
      .first();
    await expect(visiblePreview.locator('text=/subscribers/i').first()).toBeVisible();

    // Should not have DeviceFrame with gradient
    const gradientBorder = page.locator('[class*="device-frame"]');
    await expect(gradientBorder).toHaveCount(0);

    await attachFullPage(page, testInfo);
  });

  test('S10: formatting toolbar buttons are borderless and unframed (no "Windows 95" boxes)', async ({ page }, testInfo) => {
    await page.goto('/profile/creatives/new', { waitUntil: 'domcontentloaded' });
    await expect(page.getByRole('button', { name: 'Save' })).toBeVisible();

    const bold = page.getByRole('button', { name: 'Bold' });
    await expect(bold).toBeVisible();

    const borderStyle = await bold.evaluate((el) => getComputedStyle(el).borderStyle);
    expect(borderStyle).toBe('none');

    const bg = await bold.evaluate((el) => getComputedStyle(el).backgroundColor);
    const parsed = parseCssColor(bg);
    expect(parsed).not.toBeNull();
    // Inactive button background must be transparent.
    expect((parsed as ParsedColor).a).toBeLessThanOrEqual(0.01);

    await attachFullPage(page, testInfo);
  });
});

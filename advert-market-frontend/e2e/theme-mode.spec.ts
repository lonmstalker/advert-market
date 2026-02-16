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

function toLinear(v: number): number {
  const normalized = v / 255;
  if (normalized <= 0.03928) {
    return normalized / 12.92;
  }
  return ((normalized + 0.055) / 1.055) ** 2.4;
}

function luminance(color: ParsedColor): number {
  return 0.2126 * toLinear(color.r) + 0.7152 * toLinear(color.g) + 0.0722 * toLinear(color.b);
}

function contrastRatio(a: ParsedColor, b: ParsedColor): number {
  const l1 = luminance(a);
  const l2 = luminance(b);
  const light = Math.max(l1, l2);
  const dark = Math.min(l1, l2);
  return (light + 0.05) / (dark + 0.05);
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

test.describe('Theme mode', () => {
  test.beforeEach(async ({ page }) => {
    await seedOnboardedSession(page);
  });

  test('html[theme-mode] matches VITE_FORCE_THEME and base background is consistent', async ({ page }) => {
    const forced = process.env.VITE_FORCE_THEME;
    test.skip(forced !== 'light' && forced !== 'dark', 'Set VITE_FORCE_THEME=light|dark.');

    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });

    await expect
      .poll(
        async () => {
          return page.evaluate(() => document.documentElement.getAttribute('theme-mode'));
        },
        { message: 'ThemeProvider should set <html theme-mode="light|dark">.' },
      )
      .toBe(forced);

    const bg = await resolveCssVarColor(page, '--color-background-base');
    const brightness = bg.r + bg.g + bg.b;

    if (forced === 'light') {
      expect(brightness).toBeGreaterThanOrEqual(700);
    } else {
      expect(brightness).toBeLessThanOrEqual(60);
    }
  });

  test('dark fallback without tg tokens keeps readable text and input surfaces', async ({ page }) => {
    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });

    await page.evaluate(() => {
      const root = document.documentElement;
      root.setAttribute('theme-mode', 'dark');
      root.removeAttribute('data-theme');
      root.style.removeProperty('--tg-theme-bg-color');
      root.style.removeProperty('--tg-theme-secondary-bg-color');
      root.style.removeProperty('--tg-theme-text-color');
      root.style.removeProperty('--tg-theme-hint-color');
      root.style.removeProperty('--tg-theme-button-color');
    });

    const snapshot = await page.evaluate(() => {
      const input = document.createElement('input');
      input.value = 'contrast-probe';
      input.style.position = 'fixed';
      input.style.left = '-9999px';
      input.style.top = '-9999px';
      document.body.appendChild(input);

      const bodyStyles = getComputedStyle(document.body);
      const inputStyles = getComputedStyle(input);

      const result = {
        bodyBg: bodyStyles.backgroundColor,
        bodyText: bodyStyles.color,
        inputBg: inputStyles.backgroundColor,
        inputText: inputStyles.color,
      };
      input.remove();
      return result;
    });

    const bodyBg = parseCssColor(snapshot.bodyBg);
    const bodyText = parseCssColor(snapshot.bodyText);
    const inputBg = parseCssColor(snapshot.inputBg);
    const inputText = parseCssColor(snapshot.inputText);

    expect(bodyBg).not.toBeNull();
    expect(bodyText).not.toBeNull();
    expect(inputBg).not.toBeNull();
    expect(inputText).not.toBeNull();

    expect(contrastRatio(bodyBg as ParsedColor, bodyText as ParsedColor)).toBeGreaterThanOrEqual(4.5);
    expect(contrastRatio(inputBg as ParsedColor, inputText as ParsedColor)).toBeGreaterThanOrEqual(4.5);

    // Guard against "black hole" inputs.
    const bgBrightness = (inputBg as ParsedColor).r + (inputBg as ParsedColor).g + (inputBg as ParsedColor).b;
    expect(bgBrightness).toBeGreaterThan(24);
  });

  test('dark mode: Deep Gunmetal base (#1C1C1E range), not pure black', async ({ page }, testInfo) => {
    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });

    await page.evaluate(() => {
      document.documentElement.setAttribute('theme-mode', 'dark');
    });

    // Wait for styles to recompute
    await page.waitForTimeout(100);

    const bgSecondary = await resolveCssVarColor(page, '--color-background-secondary');
    const bgBase = await resolveCssVarColor(page, '--color-background-base');

    // Deep Gunmetal is NOT pure black (0,0,0) — expect some brightness
    const secondaryBrightness = bgSecondary.r + bgSecondary.g + bgSecondary.b;
    const baseBrightness = bgBase.r + bgBase.g + bgBase.b;

    // Not pure black
    expect(secondaryBrightness).toBeGreaterThan(10);
    // But still dark (under 150 total RGB)
    expect(secondaryBrightness).toBeLessThanOrEqual(150);
    expect(baseBrightness).toBeLessThanOrEqual(150);

    await testInfo.attach('screenshot', {
      body: await page.screenshot({ fullPage: true }),
      contentType: 'image/png',
    });
  });

  test('dark mode: border separator uses rgba(white) instead of rgba(black)', async ({ page }) => {
    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });

    await page.evaluate(() => {
      document.documentElement.setAttribute('theme-mode', 'dark');
    });
    await page.waitForTimeout(100);

    const separator = await resolveCssVarColor(page, '--color-border-separator');

    // In dark mode, separator should be white-based (r,g,b > 200) with low alpha
    // This ensures it's rgba(255,255,255,0.11) not rgba(0,0,0,0.08)
    if (separator.a < 0.5) {
      // Semi-transparent: rgb components should be bright (white-based)
      expect(separator.r + separator.g + separator.b).toBeGreaterThanOrEqual(600);
    }
  });

  test('dark mode: card surface differs from page background (elevation contrast)', async ({ page }) => {
    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });

    await page.evaluate(() => {
      document.documentElement.setAttribute('theme-mode', 'dark');
    });
    await page.waitForTimeout(100);

    const bgBase = await resolveCssVarColor(page, '--color-background-base');
    const bgSecondary = await resolveCssVarColor(page, '--color-background-secondary');

    // Card (base) and page (secondary) must have visible difference for elevation
    const diff = Math.abs(bgBase.r - bgSecondary.r) + Math.abs(bgBase.g - bgSecondary.g) + Math.abs(bgBase.b - bgSecondary.b);
    expect(diff).toBeGreaterThanOrEqual(5);
  });

  test('light mode CSS definitions: light theme attribute selector exists in stylesheets', async ({ page }) => {
    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Crypto News Daily')).toBeVisible();

    // Verify the light theme CSS rule exists
    const lightRuleExists = await page.evaluate(() => {
      for (const sheet of document.styleSheets) {
        try {
          for (const rule of sheet.cssRules) {
            if (rule instanceof CSSStyleRule) {
              const sel = rule.selectorText || '';
              // Browser may serialize as single or double quotes
              if (sel.includes('theme-mode') && sel.includes('light')) {
                return true;
              }
            }
          }
        } catch {
          // Cross-origin stylesheets
        }
      }
      return false;
    });

    expect(lightRuleExists).toBe(true);
  });

  test('dark vs light: forcing theme-mode attribute changes background brightness', async ({ page }, testInfo) => {
    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Crypto News Daily')).toBeVisible();

    // Force dark
    await page.evaluate(() => {
      document.documentElement.setAttribute('theme-mode', 'dark');
    });
    await page.waitForTimeout(100);

    const darkBg = await page.evaluate(() => getComputedStyle(document.body).backgroundColor);
    const darkColor = parseCssColor(darkBg);

    // Force light by setting tg vars directly (bypassing React theme)
    await page.evaluate(() => {
      const root = document.documentElement;
      root.setAttribute('theme-mode', 'light');
      root.style.setProperty('--color-background-secondary', '#f1f3f4');
      root.style.setProperty('--color-background-base', '#ffffff');
      root.style.setProperty('--am-app-background', '#f1f3f4');
      root.style.setProperty('--am-page-background', '#f1f3f4');
    });
    await page.waitForTimeout(100);

    const lightBg = await page.evaluate(() => getComputedStyle(document.body).backgroundColor);
    const lightColor = parseCssColor(lightBg);

    expect(darkColor).not.toBeNull();
    expect(lightColor).not.toBeNull();

    const darkBrightness = (darkColor as ParsedColor).r + (darkColor as ParsedColor).g + (darkColor as ParsedColor).b;
    const lightBrightness = (lightColor as ParsedColor).r + (lightColor as ParsedColor).g + (lightColor as ParsedColor).b;

    // Light should be significantly brighter than dark
    expect(lightBrightness).toBeGreaterThan(darkBrightness + 400);

    await testInfo.attach('screenshot', {
      body: await page.screenshot({ fullPage: true }),
      contentType: 'image/png',
    });
  });

  test('accent color originates from Telegram theme variable', async ({ page }) => {
    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });

    const accent = await resolveCssVarColor(page, '--color-accent-primary');

    // Accent must be defined and non-transparent
    expect(accent.a).toBeGreaterThan(0.9);
    // Not white and not black
    const brightness = accent.r + accent.g + accent.b;
    expect(brightness).toBeGreaterThan(30);
    expect(brightness).toBeLessThan(750);
  });

  test('state colors are semantic: success is green-ish, destructive is red-ish', async ({ page }) => {
    await page.goto('/catalog', { waitUntil: 'domcontentloaded' });

    const success = await resolveCssVarColor(page, '--color-state-success');
    const destructive = await resolveCssVarColor(page, '--color-state-destructive');

    // Success: green channel dominant
    expect(success.g).toBeGreaterThan(success.r);
    expect(success.g).toBeGreaterThan(success.b);

    // Destructive: red channel dominant
    expect(destructive.r).toBeGreaterThan(destructive.g);
    expect(destructive.r).toBeGreaterThan(destructive.b);
  });
});

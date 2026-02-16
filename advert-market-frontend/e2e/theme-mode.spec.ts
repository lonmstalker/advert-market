import { expect, test } from '@playwright/test';
import { seedOnboardedSession } from './helpers';

type ParsedColor = { r: number; g: number; b: number; a: number };

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

  return null;
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
});

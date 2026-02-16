import { expect, test } from '@playwright/test';
import { seedOnboardedSession } from './helpers';

type ViewportPreset = {
  name: string;
  width: number;
  height: number;
};

const VIEWPORTS: readonly ViewportPreset[] = [
  { name: 'desktop-1024', width: 1024, height: 900 },
  { name: 'desktop-1440', width: 1440, height: 900 },
  { name: 'desktop-1920', width: 1920, height: 1080 },
] as const;

async function getBox(page: import('@playwright/test').Page, selector: string) {
  return page.locator(selector).first().boundingBox();
}

async function attachDiagnostics(
  page: import('@playwright/test').Page,
  testInfo: import('@playwright/test').TestInfo,
  label: string,
) {
  const diagnostics = await page.evaluate(() => {
    const selectors = ['.am-page-stack', '.am-bottom-tabs', '.am-fixed-bottom-bar__inner'];
    const boxes = selectors.map((selector) => {
      const element = document.querySelector(selector);
      if (!element) return { selector, exists: false };
      const rect = element.getBoundingClientRect();
      return {
        selector,
        exists: true,
        left: rect.left,
        right: rect.right,
        width: rect.width,
        top: rect.top,
        bottom: rect.bottom,
      };
    });

    const cssVars = [
      '--am-page-max-width',
      '--am-page-padding',
      '--am-bottom-tabs-height',
      '--am-fixed-bottom-bar-base',
      '--am-safe-area-bottom',
    ].reduce<Record<string, string>>((acc, name) => {
      acc[name] = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
      return acc;
    }, {});

    return {
      route: window.location.pathname,
      boxes,
      cssVars,
    };
  });

  await testInfo.attach(`${label}-diagnostics`, {
    body: Buffer.from(JSON.stringify(diagnostics, null, 2), 'utf8'),
    contentType: 'application/json',
  });
  await testInfo.attach(`${label}-screenshot`, {
    body: await page.screenshot({ fullPage: true }),
    contentType: 'image/png',
  });
}

function assertAligned(leftA: number, leftB: number, rightA: number, rightB: number) {
  expect(Math.abs(leftA - leftB)).toBeLessThanOrEqual(1);
  expect(Math.abs(rightA - rightB)).toBeLessThanOrEqual(1);
}

function assertDesktopGutters(box: { x: number; width: number }, viewportWidth: number) {
  const left = box.x;
  const right = viewportWidth - (box.x + box.width);

  expect(left).toBeGreaterThanOrEqual(12);
  expect(right).toBeGreaterThanOrEqual(12);
  expect(Math.abs(left - right)).toBeLessThanOrEqual(3);
}

function assertNearFullWidth(box: { width: number }, viewportWidth: number) {
  expect(box.width / viewportWidth).toBeGreaterThanOrEqual(0.9);
}

for (const viewport of VIEWPORTS) {
  test.describe(`Shell geometry: ${viewport.name}`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test.beforeEach(async ({ page }) => {
      await seedOnboardedSession(page);
    });

    test('aligns page stack with bottom tabs on catalog and wallet', async ({ page }, testInfo) => {
      for (const route of ['/catalog', '/wallet']) {
        await page.goto(route, { waitUntil: 'domcontentloaded' });
        await expect(page.locator('.am-page-stack').first()).toBeVisible();
        await expect(page.locator('.am-bottom-tabs')).toBeVisible();

        const stack = await getBox(page, '.am-page-stack');
        const tabs = await getBox(page, '.am-bottom-tabs');

        expect(stack).not.toBeNull();
        expect(tabs).not.toBeNull();

        assertAligned(stack!.x, tabs!.x, stack!.x + stack!.width, tabs!.x + tabs!.width);
        assertDesktopGutters(stack!, viewport.width);
        assertDesktopGutters(tabs!, viewport.width);
        assertNearFullWidth(stack!, viewport.width);
        assertNearFullWidth(tabs!, viewport.width);
        await attachDiagnostics(page, testInfo, `${viewport.name}-${route.replace('/', '') || 'root'}`);
      }
    });

    test('aligns page stack with fixed bottom bar in creative editor', async ({ page }, testInfo) => {
      const cases = [{ route: '/profile/creatives/new', waitText: /^Save$/ }] as const;

      for (const scenario of cases) {
        await page.goto(scenario.route, { waitUntil: 'domcontentloaded' });
        if (scenario.route === '/profile/creatives/new') {
          await expect(page.getByRole('button', { name: scenario.waitText })).toBeVisible();
        } else {
          await expect(page.getByText(scenario.waitText)).toBeVisible();
        }
        await expect(page.locator('.am-page-stack').first()).toBeVisible();
        await expect(page.locator('.am-fixed-bottom-bar__inner')).toBeVisible();

        const stack = await getBox(page, '.am-page-stack');
        const bar = await getBox(page, '.am-fixed-bottom-bar__inner');
        const styleSnapshot = await page.evaluate(() => {
          const element = document.querySelector('.am-fixed-bottom-bar__inner');
          const outer = document.querySelector('.am-fixed-bottom-bar');
          if (!element || !outer) return null;
          const styles = getComputedStyle(element);
          const outerStyles = getComputedStyle(outer);
          const outerRect = outer.getBoundingClientRect();
          return {
            width: styles.width,
            maxWidth: styles.maxWidth,
            marginInline: styles.marginInline,
            paddingInline: styles.paddingInline,
            position: styles.position,
            outerWidth: outerStyles.width,
            outerLeft: outerStyles.left,
            outerRight: outerStyles.right,
            outerRectLeft: outerRect.left,
            outerRectRight: outerRect.right,
          };
        });

        expect(stack).not.toBeNull();
        expect(bar).not.toBeNull();

        await attachDiagnostics(page, testInfo, `${viewport.name}-${scenario.route.replaceAll('/', '-')}`);
        expect(styleSnapshot, JSON.stringify({ stack, bar, styleSnapshot })).not.toBeNull();
        assertAligned(stack!.x, bar!.x, stack!.x + stack!.width, bar!.x + bar!.width);
        assertDesktopGutters(stack!, viewport.width);
        assertDesktopGutters(bar!, viewport.width);
        assertNearFullWidth(stack!, viewport.width);
        assertNearFullWidth(bar!, viewport.width);
      }
    });
  });
}

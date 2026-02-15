import fs from 'node:fs';
import path from 'node:path';
import { expect, test } from '@playwright/test';
import { seedOnboardedSession } from './helpers';

function getCaptureDir(): string {
  const dir = process.env.CAPTURE_DIR;
  if (!dir) {
    throw new Error('CAPTURE_DIR is required (e.g. reports/design-fix/2026-02-15/before/light)');
  }
  return dir;
}

function ensureDir(dir: string): void {
  fs.mkdirSync(dir, { recursive: true });
}

const VIEWPORTS = [
  { key: 'iphone-se', width: 320, height: 568 },
  { key: 'iphone-pro-max', width: 428, height: 926 },
  { key: 'android-small', width: 360, height: 640 },
  { key: 'android-large', width: 412, height: 915 },
] as const;

function fileSafe(name: string): string {
  return name.replace(/[^a-z0-9._-]+/gi, '-').toLowerCase();
}

for (const vp of VIEWPORTS) {
  test.describe(`capture:${vp.key}`, () => {
    test.use({ viewport: { width: vp.width, height: vp.height } });

    test('onboarding -> catalog -> deal -> editor -> profile', async ({ page }) => {
      test.skip(!process.env.CAPTURE_DIR, 'CAPTURE_DIR not set (run via npm run design:capture:before:* / after:*).');
      // Capture flows include multiple full-page navigations and can be slower on WebKit; keep it stable.
      test.setTimeout(90_000);
      const outDir = getCaptureDir();
      ensureDir(outDir);

      // START SCREEN
      await page.goto('/onboarding', { waitUntil: 'domcontentloaded' });
      await page.screenshot({ path: path.join(outDir, `${vp.key}--01-onboarding.png`), fullPage: true });

      // INTEREST
      await page.getByRole('button', { name: 'Get Started' }).click();
      await page.waitForURL('**/onboarding/interest');
      await page.screenshot({ path: path.join(outDir, `${vp.key}--02-interest.png`), fullPage: true });

      await page.getByRole('button', { name: /advertiser/i }).click();

      // TOUR SLIDE 1 (catalog mockup)
      await page.getByRole('button', { name: 'Continue' }).click();
      await page.waitForURL('**/onboarding/tour');
      await page.screenshot({ path: path.join(outDir, `${vp.key}--03-tour-slide1.png`), fullPage: true });

      // Slide 1: open details mockup (also completes task 1 in current flow)
      await page.getByText('Crypto News Daily', { exact: true }).click();
      await page.screenshot({ path: path.join(outDir, `${vp.key}--04-tour-slide1-detail.png`), fullPage: true });

      // TOUR SLIDE 2 (deal mockup)
      await page.getByRole('button', { name: 'Next' }).click();
      await page.screenshot({ path: path.join(outDir, `${vp.key}--05-tour-slide2.png`), fullPage: true });

      // Slide 2: approve
      await page.getByRole('button', { name: 'Approve' }).click();
      await page.screenshot({ path: path.join(outDir, `${vp.key}--06-tour-slide2-approved.png`), fullPage: true });

      // Slide 2: all states
      await page.getByText('All 17 states').click();
      await page.screenshot({ path: path.join(outDir, `${vp.key}--07-tour-slide2-allstates.png`), fullPage: true });
      await page.getByText('Back to deal').click();

      // TOUR SLIDE 3 (wallet mockup)
      await page.getByRole('button', { name: 'Next' }).click();
      await page.screenshot({ path: path.join(outDir, `${vp.key}--08-tour-slide3.png`), fullPage: true });

      // For the rest of the app screens, seed an onboarded session. This keeps capture stable
      // even if the onboarding completion request is slow/flaky across browser engines.
      await seedOnboardedSession(page);

      await page.goto('/catalog', { waitUntil: 'domcontentloaded' });
      await expect(page.getByPlaceholder('Search channels...')).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${vp.key}--09-catalog.png`), fullPage: true });

      // Channel detail (real page)
      await page.goto('/catalog/channels/5', { waitUntil: 'domcontentloaded' });
      await expect(page.getByRole('button', { name: 'Share' })).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${vp.key}--10-channel-detail.png`), fullPage: true });

      // Deal detail (real page)
      await page.goto('/deals/deal-1', { waitUntil: 'domcontentloaded' });
      await expect(page.getByRole('list', { name: 'Timeline' })).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${vp.key}--11-deal-detail.png`), fullPage: true });

      // Creative editor (real page)
      await page.goto('/profile/creatives/new', { waitUntil: 'domcontentloaded' });
      await expect(page.getByRole('button', { name: 'Save' })).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${vp.key}--12-creative-editor.png`), fullPage: true });
      await page.getByText(/^Preview$/).click();
      await page.screenshot({ path: path.join(outDir, `${vp.key}--13-creative-preview.png`), fullPage: true });

      // Profile page
      await page.goto('/profile', { waitUntil: 'domcontentloaded' });
      await expect(page.getByText(/^Settings$/)).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${vp.key}--14-profile.png`), fullPage: true });

      // Extra: write the theme to a marker file to make comparisons easier.
      const forcedTheme = process.env.VITE_FORCE_THEME ? String(process.env.VITE_FORCE_THEME) : 'auto';
      fs.writeFileSync(path.join(outDir, `${fileSafe(vp.key)}--theme.txt`), `${forcedTheme}\n`, 'utf8');
    });
  });
}

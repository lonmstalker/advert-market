import fs from 'node:fs';
import path from 'node:path';
import { expect, test } from '@playwright/test';
import { seedOnboardedSession } from './helpers';

type CaptureViewport = {
  key: string;
  width: number;
  height: number;
};

const VIEWPORTS: readonly CaptureViewport[] = [
  { key: 'iphone-se', width: 320, height: 568 },
  { key: 'iphone-pro-max', width: 428, height: 926 },
  { key: 'android-small', width: 360, height: 640 },
  { key: 'android-large', width: 412, height: 915 },
] as const;

function getCaptureDir(): string {
  const dir = process.env.CAPTURE_DIR;
  if (!dir) {
    throw new Error('CAPTURE_DIR is required (e.g. reports/design-fix/2026-02-15/after/dark)');
  }
  return dir;
}

function ensureDir(dir: string): void {
  fs.mkdirSync(dir, { recursive: true });
}

for (const viewport of VIEWPORTS) {
  test.describe(`capture:${viewport.key}`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('captures locale-first onboarding + unified telegram-native pages', async ({ page }) => {
      test.skip(!process.env.CAPTURE_DIR, 'CAPTURE_DIR not set (run via npm run design:capture:*).');
      test.setTimeout(90_000);

      const outDir = getCaptureDir();
      ensureDir(outDir);

      await page.goto('/onboarding', { waitUntil: 'domcontentloaded' });
      await expect(page.getByTestId('onboarding-locale-step')).toBeVisible();
      await expect(page.getByTestId('onboarding-locale-logo')).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${viewport.key}--01-onboarding-locale.png`), fullPage: true });

      await page.getByRole('button', { name: 'Continue' }).click();
      await expect(page.getByTestId('onboarding-welcome-step')).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${viewport.key}--02-onboarding-welcome.png`), fullPage: true });

      await seedOnboardedSession(page);

      await page.goto('/catalog', { waitUntil: 'domcontentloaded' });
      await expect(page.getByTestId('catalog-page-shell')).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${viewport.key}--03-catalog.png`), fullPage: true });

      await page.goto('/deals', { waitUntil: 'domcontentloaded' });
      await expect(page.getByTestId('deals-page-shell')).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${viewport.key}--04-deals.png`), fullPage: true });

      await page.goto('/wallet', { waitUntil: 'domcontentloaded' });
      await expect(page.getByTestId('wallet-page-shell')).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${viewport.key}--05-wallet.png`), fullPage: true });

      await page.goto('/wallet/history', { waitUntil: 'domcontentloaded' });
      await expect(page.getByTestId('wallet-history-page-shell')).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${viewport.key}--06-wallet-history.png`), fullPage: true });

      await page.goto('/wallet/history/tx-1', { waitUntil: 'domcontentloaded' });
      await expect(page.getByTestId('wallet-transaction-page-shell')).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${viewport.key}--07-wallet-transaction.png`), fullPage: true });

      await page.goto('/profile', { waitUntil: 'domcontentloaded' });
      await expect(page.getByText(/^(Settings|Настройки)$/)).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${viewport.key}--08-profile.png`), fullPage: true });

      await page.goto('/profile/locale-currency', { waitUntil: 'domcontentloaded' });
      await expect(page.getByText(/^(Language & Currency|Язык и валюта)$/)).toBeVisible();
      await page.screenshot({ path: path.join(outDir, `${viewport.key}--09-profile-locale-currency.png`), fullPage: true });
    });
  });
}

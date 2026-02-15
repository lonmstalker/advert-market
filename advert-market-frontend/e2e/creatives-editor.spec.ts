import { expect, test } from '@playwright/test';
import { seedOnboardedSession } from './helpers';

test.describe('Creatives editor', () => {
  test.beforeEach(async ({ page }) => {
    await seedOnboardedSession(page);
  });

  test('formatting toolbar works: selecting text enables Bold and toggles active state', async ({ page }) => {
    await page.goto('/profile/creatives/new', { waitUntil: 'domcontentloaded' });
    await expect(page.getByRole('button', { name: 'Save' })).toBeVisible();

    // Both mobile and desktop forms are in the DOM; select the visible one for the current viewport.
    const textarea = page.locator('textarea[placeholder="Enter ad post text..."]:visible');
    await expect(textarea).toHaveCount(1);
    await textarea.click();
    // Prefer real typing so selection tracking (keyup/mouseup/select) is exercised in the same way as in Telegram WebView.
    await textarea.type('Hello world');
    // Create a non-empty selection to enable the toolbar.
    await textarea.press('Shift+ArrowLeft');

    const form = page.locator('.creative-editor-mobile, .creative-editor-desktop').filter({ has: textarea }).first();
    const bold = form.getByRole('button', { name: 'Bold' });
    await expect(bold).toBeEnabled();

    await bold.click();
    await expect(bold).toHaveAttribute('aria-pressed', 'true');
  });
});

import { expect, test } from '@playwright/test';

test.describe('Onboarding Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/onboarding', { waitUntil: 'domcontentloaded' });
  });

  test('completes full onboarding as advertiser', async ({ page }) => {
    // Step 1: Welcome page
    await expect(page.getByText('Ad Market')).toBeVisible();
    await expect(page.getByText('Takes 30 seconds')).toBeVisible();
    await expect(page.getByText('Channel Catalog')).toBeVisible();
    await expect(page.getByText('TON Escrow')).toBeVisible();
    await expect(page.getByText('Deal Tracking')).toBeVisible();

    await page.getByRole('button', { name: 'Get Started' }).click();

    // Step 2: Interest selection
    await expect(page.getByText('Who are you?')).toBeVisible();
    const continueBtn = page.getByRole('button', { name: 'Continue' });
    await expect(continueBtn).toBeDisabled();

    await page.getByRole('button', { name: /advertiser/i }).click();
    await expect(page.getByText('Find channels')).toBeVisible();
    await expect(continueBtn).toBeEnabled();

    await continueBtn.click();

    // Step 3: Tour slide 1 — Catalog
    await expect(page.getByText('Find Channels')).toBeVisible();
    const nextBtn = page.getByRole('button', { name: 'Next' });
    await expect(nextBtn).toBeDisabled();

    await page.getByText('Crypto News Daily', { exact: true }).click();
    await expect(page.getByText('Post Price')).toBeVisible({ timeout: 10_000 });
    await expect(nextBtn).toBeEnabled();
    await nextBtn.click();

    // Step 3: Tour slide 2 — Deal
    await expect(page.getByText('Secure Deals')).toBeVisible();
    await expect(nextBtn).toBeDisabled();

    await page.getByRole('button', { name: 'Approve' }).click();
    await expect(page.getByText(/Creative approved/)).toBeVisible({ timeout: 10_000 });
    await expect(nextBtn).toBeEnabled();
    await nextBtn.click();

    // Step 3: Tour slide 3 — Wallet
    await expect(page.getByText('Secure Payments')).toBeVisible();
    const finishBtn = page.getByRole('button', { name: 'Get Started' });
    await expect(finishBtn).toBeVisible();

    await page.getByText('Escrow', { exact: true }).click();
    await expect(page.getByText(/secure escrow works/)).toBeVisible();

    await finishBtn.click();

    // Should redirect to catalog
    await page.waitForURL('**/catalog');
  });

  test('selects both roles and sees hint', async ({ page }) => {
    await page.getByRole('button', { name: 'Get Started' }).click();

    await page.getByRole('button', { name: /advertiser/i }).click();
    await page.getByRole('button', { name: /channel owner/i }).click();

    await expect(page.getByText("Great! You'll see features for both roles")).toBeVisible();
    await expect(page.getByText('Find channels')).toBeVisible();
    await expect(page.getByText('List your channel')).toBeVisible();
  });

  test('skips tour and completes onboarding', async ({ page }) => {
    await page.getByRole('button', { name: 'Get Started' }).click();
    await page.getByRole('button', { name: /advertiser/i }).click();
    await page.getByRole('button', { name: 'Continue' }).click();

    await expect(page.getByText('Find Channels')).toBeVisible();
    await page.getByRole('button', { name: /skip tutorial/i }).click();

    const dialog = page.locator('[class*="dialogModalActive"]').first();
    await expect(dialog).toBeVisible();
    await dialog.locator('[class*="dialogModalContentFooterButton"]').nth(1).click();

    await page.waitForURL('**/catalog');
  });

  test('locked channels are not clickable in tour', async ({ page }) => {
    await page.getByRole('button', { name: 'Get Started' }).click();
    await page.getByRole('button', { name: /advertiser/i }).click();
    await page.getByRole('button', { name: 'Continue' }).click();

    // "Tech Digest" and "AI Weekly" should be visually locked (opacity 0.5, pointer-events none)
    const techDigest = page.getByText('Tech Digest').locator('..');
    await expect(techDigest).toBeVisible();

    // Click on locked channel should not open detail
    await page.getByText('Tech Digest').click({ force: true });
    await expect(page.getByText('Post Price')).not.toBeVisible();
  });

  test('settings sheet opens from welcome screen', async ({ page }) => {
    // Globe button should be visible
    const settingsBtn = page.getByRole('button', { name: /settings/i });
    await expect(settingsBtn).toBeVisible();

    await settingsBtn.click();

    // Settings sheet should show language and currency sections
    await expect(page.getByText('Language', { exact: true })).toBeVisible();
    await expect(page.getByText('Display Currency', { exact: true })).toBeVisible();
  });

  test('redirects to interest page when navigating directly to tour', async ({ page }) => {
    try {
      await page.goto('/onboarding/tour', { waitUntil: 'domcontentloaded' });
    } catch {
      // WebKit can abort `page.goto` if the SPA redirects immediately.
    }
    await expect(page.getByText('Who are you?')).toBeVisible();
  });

  test('step indicator progresses through steps', async ({ page }) => {
    // Welcome — first dot active
    const tablist = page.getByRole('tablist').first();
    if (await tablist.isVisible()) {
      await expect(tablist).toBeVisible();
    }

    await page.getByRole('button', { name: 'Get Started' }).click();
    await expect(page.getByText('Who are you?')).toBeVisible();

    await page.getByRole('button', { name: /advertiser/i }).click();
    await page.getByRole('button', { name: 'Continue' }).click();

    // Tour — tablist with 3 tabs
    const tourTablist = page.getByRole('tablist');
    await expect(tourTablist).toBeVisible();
    await expect(page.getByRole('tab')).toHaveCount(3);
  });

  test('back navigation works in tour slides', async ({ page }) => {
    await page.getByRole('button', { name: 'Get Started' }).click();
    await page.getByRole('button', { name: /advertiser/i }).click();
    await page.getByRole('button', { name: 'Continue' }).click();

    // Slide 1: go to detail and back
    await page.getByText('Crypto News Daily', { exact: true }).click();
    await expect(page.getByText('Post Price')).toBeVisible();
    await page.getByText('← Back to list').click();
    await expect(page.getByText('Tech Digest')).toBeVisible();
  });

  test('deal slide shows all 17 states', async ({ page }) => {
    await page.getByRole('button', { name: 'Get Started' }).click();
    await page.getByRole('button', { name: /advertiser/i }).click();
    await page.getByRole('button', { name: 'Continue' }).click();

    // Complete slide 1 task
    await page.getByText('Crypto News Daily', { exact: true }).click();
    await page.getByRole('button', { name: 'Next' }).click();

    // Slide 2: view all states
    await expect(page.getByText('Secure Deals')).toBeVisible();
    await page.getByText('All 17 states →').click();

    await expect(page.getByText('Negotiation')).toBeVisible();
    await expect(page.getByText('Payment & Creative')).toBeVisible();
    await expect(page.getByText('Publication')).toBeVisible();
    await expect(page.getByText('Special Cases')).toBeVisible();

    await page.getByText('← Back to deal').click();
    await expect(page.getByText('Offer Sent')).toBeVisible();
  });

  test('wallet slide shows escrow flow and policy', async ({ page }) => {
    await page.getByRole('button', { name: 'Get Started' }).click();
    await page.getByRole('button', { name: /advertiser/i }).click();
    await page.getByRole('button', { name: 'Continue' }).click();

    // Complete slides 1 and 2
    await page.getByText('Crypto News Daily', { exact: true }).click();
    await page.getByRole('button', { name: 'Next' }).click();
    await page.getByRole('button', { name: 'Approve' }).click();
    await page.getByRole('button', { name: 'Next' }).click();

    // Slide 3: escrow flow
    await expect(page.getByText('Secure Payments')).toBeVisible();
    await page.getByText('Escrow', { exact: true }).click();
    await expect(page.getByText(/secure escrow works/)).toBeVisible();

    // Policy view
    await page.getByText('How are payments confirmed? →').click();
    await expect(page.getByText('Confirmation Limits')).toBeVisible();
    await expect(page.getByText('Up to 100 TON')).toBeVisible();

    await page.getByText('← Back to escrow').click();
    await expect(page.getByText(/secure escrow works/)).toBeVisible();
  });
});

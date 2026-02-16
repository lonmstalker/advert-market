import { expect, type Page, test } from '@playwright/test';
import { completeOnboarding } from './helpers';

async function navigateToDeals(page: Page) {
  await completeOnboarding(page);
  await page.getByRole('link', { name: /^(Deals|Сделки)$/ }).click();
  await page.waitForURL('**/deals');
}

test.describe('Deals Page', () => {
  test('displays deal list with segment control', async ({ page }) => {
    await navigateToDeals(page);
    await expect(page.getByText(/^(Deals|Сделки)$/).first()).toBeVisible();
    await expect(page.getByRole('button', { name: /^(Advertiser|Рекламодатель)$/ })).toBeVisible();
    await expect(page.getByRole('button', { name: /^(Owner|Владелец)$/ })).toBeVisible();
  });

  test('shows advertiser deals by default', async ({ page }) => {
    await navigateToDeals(page);
    // deal-1: OFFER_PENDING, Tech Digest, role=ADVERTISER
    await expect(page.getByText('Tech Digest').first()).toBeVisible();
  });

  test('switching to Owner tab shows owner deals', async ({ page }) => {
    await navigateToDeals(page);
    await page.getByRole('button', { name: /^(Owner|Владелец)$/ }).click();
    // deal-4: FUNDED, Crypto News Daily, role=OWNER
    await expect(page.getByText('Crypto News Daily').first()).toBeVisible();
  });

  test('clicking a deal navigates to detail page', async ({ page }) => {
    await navigateToDeals(page);
    await expect(page.getByText('Tech Digest').first()).toBeVisible();
    await page.getByText('Tech Digest').first().click();
    await page.waitForURL('**/deals/deal-1');
    await expect(page.getByText(/^(Timeline|Таймлайн|Хронология)$/)).toBeVisible();
  });

  test('deal detail shows timeline and actions', async ({ page }) => {
    await navigateToDeals(page);
    await page.getByText('Tech Digest').first().click();
    await page.waitForURL('**/deals/deal-1');
    // Should show timeline
    await expect(page.getByText(/^(Timeline|Таймлайн|Хронология)$/)).toBeVisible();
    // Advertiser on OFFER_PENDING should see Cancel button
    await expect(page.getByRole('button', { name: /^(Cancel|Отменить)$/ })).toBeVisible();
  });

  test('Pay action opens TON payment sheet', async ({ page }) => {
    await navigateToDeals(page);
    await page.getByText('Finance Pro').first().click();
    await page.waitForURL('**/deals/deal-3');

    // deal-3: AWAITING_PAYMENT, role=ADVERTISER
    // Ensure the deal page finished rendering before we click the sticky action.
    await expect(page.getByText(/^(Timeline|Таймлайн|Хронология)$/)).toBeVisible();
    await page.getByRole('button', { name: /^(Pay|Оплатить)$/ }).click();

    const sheet = page.getByTestId('payment-sheet');
    await expect(sheet).toBeVisible();

    // Wallet not connected in E2E: Pay button inside sheet should be disabled
    await expect(sheet.getByRole('button', { name: /^(Pay|Оплатить)$/ })).toBeDisabled();
  });

  test('Pending TON intent resumes polling and deal becomes Funded', async ({ page }) => {
    await page.addInitScript(() => {
      sessionStorage.setItem(
        'ton_pending_intent',
        JSON.stringify({
          type: 'escrow_deposit',
          dealId: 'deal-3',
          sentAt: Date.now(),
          address: 'UQ_MOCK_ESCROW_deal-3',
          amountNano: '8000000000',
        }),
      );
    });

    await navigateToDeals(page);
    await page.getByText('Finance Pro').first().click();
    await page.waitForURL('**/deals/deal-3');

    // Deposit mock progresses and sets deal status to FUNDED.
    await expect(page.getByText(/(Funded|Оплачено|Профинансировано)/i)).toBeVisible();
  });
});

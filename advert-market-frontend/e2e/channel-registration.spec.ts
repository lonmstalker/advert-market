import { expect, test } from '@playwright/test';
import { seedOnboardedSession } from './helpers';

test.describe('Channel Registration Flow', () => {
  test.beforeEach(async ({ page }) => {
    await seedOnboardedSession(page);
  });

  test('step 1: verify channel and see channel info', async ({ page }) => {
    await page.goto('/profile/channels/new', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText(/^(Add Channel|Добавить канал)$/)).toBeVisible();

    const input = page.getByPlaceholder('@username');
    await input.fill('mynewchannel');
    await page.getByRole('button', { name: /^(Verify|Проверить)$/ }).click();

    await expect(page.getByText(/Channel @mynewchannel/)).toBeVisible();
    await expect(page.getByText(/^(500 subscribers|500 подписчиков)$/)).toBeVisible();
  });

  test('step 1: channel not found error', async ({ page }) => {
    await page.goto('/profile/channels/new', { waitUntil: 'domcontentloaded' });

    const input = page.getByPlaceholder('@username');
    await input.fill('nonexistent_channel');
    await page.getByRole('button', { name: /^(Verify|Проверить)$/ }).click();

    await expect(page.getByText(/^(Channel not found|Канал не найден)$/)).toBeVisible();
  });

  test('step 1: bot not admin error', async ({ page }) => {
    await page.goto('/profile/channels/new', { waitUntil: 'domcontentloaded' });

    const input = page.getByPlaceholder('@username');
    await input.fill('no_bot_channel');
    await page.getByRole('button', { name: /^(Verify|Проверить)$/ }).click();

    await expect(page.getByText(/(Bot is not added as a channel admin|Бот не добавлен администратором)/i)).toBeVisible();
  });

  test('step 1: already registered channel redirects to profile', async ({ page }) => {
    await page.goto('/profile/channels/new', { waitUntil: 'domcontentloaded' });

    const input = page.getByPlaceholder('@username');
    await input.fill('cryptonewsdaily');
    await page.getByRole('button', { name: /^(Verify|Проверить)$/ }).click();

    await page.waitForURL('**/profile');
  });

  test('full flow: verify, register, redirect to profile', async ({ page }) => {
    await page.goto('/profile/channels/new', { waitUntil: 'domcontentloaded' });

    // Step 1: Verify
    const input = page.getByPlaceholder('@username');
    await input.fill('mynewchannel');
    await page.getByRole('button', { name: /^(Verify|Проверить)$/ }).click();

    await expect(page.getByText(/Channel @mynewchannel/)).toBeVisible();

    // Step 2: Register
    await page.getByRole('button', { name: /^(Register|Зарегистрировать)$/ }).click();

    // Should redirect to profile
    await page.waitForURL('**/profile', { timeout: 10000 });
  });
});

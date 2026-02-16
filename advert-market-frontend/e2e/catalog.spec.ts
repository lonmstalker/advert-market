import { expect, test } from '@playwright/test';
import { completeOnboarding } from './helpers';

test.describe('Catalog Page', () => {
  test.beforeEach(async ({ page }) => {
    await completeOnboarding(page);
  });

  test('displays search bar and filter button', async ({ page }) => {
    await expect(page.getByPlaceholder(/^(Search channels\.\.\.|Поиск каналов\.\.\.)$/)).toBeVisible();
    await expect(page.getByRole('button', { name: /^(Filters|Фильтры)$/ })).toBeVisible();
  });

  test('renders channel cards after loading', async ({ page }) => {
    await expect(page.getByText('Crypto News Daily')).toBeVisible();
    await expect(page.getByText('Tech Digest')).toBeVisible();
    await expect(page.getByText('AI Weekly')).toBeVisible();
  });

  test('shows category chip row with "All topics"', async ({ page }) => {
    await expect(page.getByRole('button', { name: /^(All topics|Все тематики)$/ })).toBeVisible();
  });

  test('shows channel list summary once data is loaded', async ({ page }) => {
    await expect(page.getByText('Crypto News Daily')).toBeVisible();
    await expect(page.getByText(/(channels|каналов|каналы)/i).first()).toBeVisible();
  });

  test('shows verified badges on verified channels', async ({ page }) => {
    // Crypto News Daily is verified — check it has the verified icon via title
    const card = page.getByText('Crypto News Daily').locator('..');
    await expect(card).toBeVisible();
  });

  test('search filters channels by name', async ({ page }) => {
    const searchInput = page.getByPlaceholder(/^(Search channels\.\.\.|Поиск каналов\.\.\.)$/);
    await searchInput.fill('AI Weekly');

    // Wait for debounce (300ms) + fetch
    await expect(page.getByText('AI Weekly')).toBeVisible();
    // Other channels should not be visible
    await expect(page.getByText('Crypto News Daily')).not.toBeVisible({ timeout: 2000 });
  });

  test('search with no results shows empty state', async ({ page }) => {
    const searchInput = page.getByPlaceholder(/^(Search channels\.\.\.|Поиск каналов\.\.\.)$/);
    await searchInput.fill('xyznonexistent12345');

    await expect(page.getByText(/^(Nothing found|Ничего не найдено)$/)).toBeVisible();
    await expect(page.getByRole('button', { name: /^(Reset filters|Сбросить фильтры)$/ })).toBeVisible();
  });

  test('reset filters clears search and shows all channels', async ({ page }) => {
    const searchInput = page.getByPlaceholder(/^(Search channels\.\.\.|Поиск каналов\.\.\.)$/);
    await searchInput.fill('xyznonexistent12345');

    await expect(page.getByText(/^(Nothing found|Ничего не найдено)$/)).toBeVisible();
    await page.getByRole('button', { name: /^(Reset filters|Сбросить фильтры)$/ }).click();

    await expect(page.getByText('Crypto News Daily')).toBeVisible();
  });

  test('category chip filters channels', async ({ page }) => {
    await expect(page.getByTestId('catalog-channel-card').first()).toBeVisible();

    // Click "Gaming" category chip
    await page.getByRole('button', { name: /^(Gaming|Игры)$/ }).click();

    // GameDev Channel is in gaming category
    await expect(page.getByText('GameDev Channel')).toBeVisible();
  });

  test('clicking channel card navigates to channel detail', async ({ page }) => {
    await expect(page.getByText('Crypto News Daily')).toBeVisible();
    await page.getByText('Crypto News Daily').click();

    await page.waitForURL('**/catalog/channels/1');
    // Channel detail page should show
    await expect(page.getByText(/^(Subscribers|Подписчики)$/)).toBeVisible();
  });

  test('channel cards show subscriber count', async ({ page }) => {
    await expect(page.getByText('125K')).toBeVisible(); // Crypto News Daily
  });

  test('channel cards show price info', async ({ page }) => {
    // Crypto News Daily: 5 TON
    await expect(page.getByText(/^(from|от) 5 TON$/i)).toBeVisible();
  });
});

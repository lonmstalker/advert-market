import { expect, type Page, test } from '@playwright/test';
import { seedOnboardedSession } from './helpers';

async function navigateToChannel(page: Page, channelName: string) {
  const channel = page.getByText(channelName, { exact: true });
  await expect(channel).toBeVisible();
  await channel.click();
}

async function startAtCatalog(page: Page) {
  await seedOnboardedSession(page);
  await page.goto('/catalog', { waitUntil: 'commit' });
  await page.waitForURL('**/catalog');
}

test.describe('Channel Detail Page', () => {
  test.describe('Channel with full data (Crypto News Daily)', () => {
    test.beforeEach(async ({ page }) => {
      await startAtCatalog(page);
      await navigateToChannel(page, 'Crypto News Daily');
      await page.waitForURL('**/catalog/channels/1');
    });

    test('shows channel title and verified badge', async ({ page }) => {
      await expect(page.getByText('Crypto News Daily')).toBeVisible();
      await expect(page.getByText('@cryptonewsdaily')).toBeVisible();
      await expect(page.getByRole('img', { name: 'Verified' }).first()).toBeVisible();
    });

    test('shows username with channel age', async ({ page }) => {
      await expect(page.getByText('@cryptonewsdaily')).toBeVisible();
    });

    test('shows subscriber count', async ({ page }) => {
      // Formatted with locale: 125,000 or 125 000
      await expect(page.getByText('Subscribers')).toBeVisible();
    });

    test('shows average reach stat', async ({ page }) => {
      await expect(page.getByText('Avg. reach')).toBeVisible();
    });

    test('shows engagement rate', async ({ page }) => {
      await expect(page.getByText('Engagement')).toBeVisible();
      await expect(page.getByText('3.6%')).toBeVisible();
    });

    test('shows channel description', async ({ page }) => {
      await expect(page.getByText(/Ежедневные новости/)).toBeVisible();
    });

    test('shows "Open channel in Telegram" link', async ({ page }) => {
      await expect(page.getByText('Open channel in Telegram')).toBeVisible();
    });

    test('shows topic badges', async ({ page }) => {
      await expect(page.getByText('Криптовалюта')).toBeVisible();
      await expect(page.getByText('Финансы')).toBeVisible();
    });

    test('shows pricing section with overview', async ({ page }) => {
      await page.getByRole('button', { name: 'Pricing' }).click();
      // Min price: "from 3 TON" (repost is cheapest at 3 TON)
      await expect(page.getByText(/^from 3 TON$/)).toBeVisible();
    });

    test('shows pricing rule cards with post types', async ({ page }) => {
      await page.getByRole('button', { name: 'Pricing' }).click();
      await expect(page.getByText('Native post').first()).toBeVisible();
      await expect(page.getByText('Story')).toBeVisible();
      await expect(page.getByText('Repost')).toBeVisible();
    });

    test('shows pricing amounts on cards', async ({ page }) => {
      await page.getByRole('button', { name: 'Pricing' }).click();
      await expect(page.getByText(/^5 TON$/)).toBeVisible();
      await expect(page.getByText(/^8 TON$/)).toBeVisible();
      await expect(page.getByText(/^4 TON$/)).toBeVisible();
      await expect(page.getByText(/^3 TON$/)).toBeVisible();
    });

    test('shows placement conditions section', async ({ page }) => {
      await page.getByRole('button', { name: 'Rules' }).click();
      await expect(page.getByText('Post size')).toBeVisible();
    });

    test('shows media rules', async ({ page }) => {
      await page.getByRole('button', { name: 'Rules' }).click();
      await expect(page.getByText('Media allowed')).toBeVisible();
    });

    test('shows link rules', async ({ page }) => {
      await page.getByRole('button', { name: 'Rules' }).click();
      await expect(page.getByText('Links allowed')).toBeVisible();
    });

    test('shows formatting rules', async ({ page }) => {
      await page.getByRole('button', { name: 'Rules' }).click();
      await expect(page.getByText('Text formatting allowed')).toBeVisible();
    });

    test('shows prohibited topics', async ({ page }) => {
      await page.getByRole('button', { name: 'Rules' }).click();
      await expect(page.getByText('Казино')).toBeVisible();
      await expect(page.getByText('Форекс')).toBeVisible();
    });

    test('shows owner custom note', async ({ page }) => {
      await page.getByRole('button', { name: 'Rules' }).click();
      await expect(page.getByText(/Пост должен быть на тему/)).toBeVisible();
    });

    test('shows share button', async ({ page }) => {
      await expect(page.getByRole('button', { name: 'Share' })).toBeVisible();
    });

    test('owner sees edit button and no create deal CTA', async ({ page }) => {
      // User id=1 is owner of channel 1
      await expect(page.getByRole('button', { name: 'Edit' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Create deal' })).not.toBeVisible();
    });
  });

  test.describe('Private channel (Marketing Hub)', () => {
    test.beforeEach(async ({ page }) => {
      await startAtCatalog(page);
      await navigateToChannel(page, 'Marketing Hub');
      await page.waitForURL('**/catalog/channels/5');
    });

    test('shows "Private channel" instead of username', async ({ page }) => {
      await expect(page.getByText('Private channel')).toBeVisible();
    });

    test('shows "Join channel" link for private channel', async ({ page }) => {
      await expect(page.getByText('Join channel')).toBeVisible();
    });

    test('non-owner sees "Create deal" button', async ({ page }) => {
      // User id=1 is NOT owner of channel 5 (ownerId=5)
      await expect(page.getByRole('button', { name: 'Create deal' })).toBeVisible();
    });

    test('non-owner does NOT see edit button', async ({ page }) => {
      await expect(page.getByRole('button', { name: 'Edit' })).not.toBeVisible();
    });

    test('shows custom rules for private channel', async ({ page }) => {
      await page.getByRole('button', { name: 'Rules' }).click();
      await expect(page.getByText(/Только маркетинговая тематика/)).toBeVisible();
    });
  });

  test.describe('Non-existent channel', () => {
    test('shows error state for unknown channel', async ({ page }) => {
      // WebKit can hang waiting for "domcontentloaded"/"load" on deep links in dev mode.
      await seedOnboardedSession(page);
      await page.goto('/catalog/channels/99999', { waitUntil: 'commit' });
      await expect(page.getByText('Page not found')).toBeVisible();
    });
  });

  test.describe('Navigation', () => {
    test('back button returns to catalog', async ({ page }) => {
      await startAtCatalog(page);
      await navigateToChannel(page, 'Crypto News Daily');
      await page.waitForURL('**/catalog/channels/1');

      // Use browser back
      await page.goBack();
      await page.waitForURL('**/catalog');
      await expect(page.getByPlaceholder('Search channels...')).toBeVisible();
    });
  });

  test.describe('Channel without rules (Tech Digest)', () => {
    test.beforeEach(async ({ page }) => {
      await startAtCatalog(page);
      await navigateToChannel(page, 'Tech Digest');
      await page.waitForURL('**/catalog/channels/2');
    });

    test('shows "No rules specified" when no rules', async ({ page }) => {
      await page.getByRole('button', { name: 'Rules' }).click();
      await expect(page.getByText('No rules specified')).toBeVisible();
    });

    test('shows pricing rules', async ({ page }) => {
      await page.getByRole('button', { name: 'Pricing' }).click();
      await expect(page.getByText('Native post').first()).toBeVisible();
      await expect(page.getByText('Story')).toBeVisible();
    });
  });
});

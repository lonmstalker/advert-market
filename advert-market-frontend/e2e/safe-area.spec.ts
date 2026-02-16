import { expect, test } from '@playwright/test';
import { seedOnboardedSession } from './helpers';

async function setSafeAreaBottom(page: import('@playwright/test').Page, px: number) {
  await page.addStyleTag({ content: `:root { --am-safe-area-bottom: ${px}px !important; }` });
  await expect
    .poll(() =>
      page.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--am-safe-area-bottom').trim()),
    )
    .toBe(`${px}px`);
}

async function distanceFromViewportBottom(locator: import('@playwright/test').Locator) {
  const box = await locator.boundingBox();
  expect(box).toBeTruthy();
  if (!box) return 0;

  const viewport = locator.page().viewportSize();
  expect(viewport).toBeTruthy();
  if (!viewport) return 0;

  return viewport.height - (box.y + box.height);
}

async function scrollToBottom(page: import('@playwright/test').Page) {
  await page.evaluate(() => {
    const root = document.querySelector('#root') as HTMLElement | null;
    const target =
      root && root.scrollHeight > root.clientHeight + 1
        ? root
        : (document.scrollingElement as HTMLElement | null) ?? document.documentElement;
    target.scrollTo(0, target.scrollHeight);
  });
}

test.describe('Safe area (fixed-bottom bars)', () => {
  test.beforeEach(async ({ page }) => {
    await seedOnboardedSession(page);
  });

  test('S4: channel detail CTA respects safe area', async ({ page }) => {
    await page.goto('/catalog/channels/5', { waitUntil: 'domcontentloaded' });
    await setSafeAreaBottom(page, 34);
    const cta = page.getByRole('button', { name: /^(Create deal|Создать сделку)$/ });
    await expect(cta).toBeVisible();

    const d = await distanceFromViewportBottom(cta);
    expect(d).toBeGreaterThanOrEqual(49);
  });

  test('S7: deal detail actions respect safe area and do not overlap timeline', async ({ page }) => {
    await page.goto('/deals/deal-1', { waitUntil: 'domcontentloaded' });
    await setSafeAreaBottom(page, 34);
    const action = page.getByRole('button', { name: /^(Cancel|Отменить)$/ });
    await expect(action).toBeVisible();

    const d = await distanceFromViewportBottom(action);
    expect(d).toBeGreaterThanOrEqual(49);

    // Scroll to the bottom and ensure the last timeline step isn't behind the fixed actions bar.
    await scrollToBottom(page);

    const timeline = page.getByRole('list', { name: /^(Timeline|Таймлайн|Хронология)$/ });
    await expect(timeline).toBeVisible();

    const lastStep = timeline.locator('li').last();
    const lastBox = await lastStep.boundingBox();
    const actionBox = await action.boundingBox();
    expect(lastBox).toBeTruthy();
    expect(actionBox).toBeTruthy();
    if (lastBox && actionBox) {
      expect(lastBox.y + lastBox.height).toBeLessThanOrEqual(actionBox.y - 8);
    }
  });

  test('S10: creative editor save respects safe area and does not overlap form', async ({ page }) => {
    await page.goto('/profile/creatives/new', { waitUntil: 'domcontentloaded' });
    await setSafeAreaBottom(page, 34);
    const save = page.getByRole('button', { name: /^(Save|Сохранить)$/ });
    await expect(save).toBeVisible();

    const d = await distanceFromViewportBottom(save);
    expect(d).toBeGreaterThanOrEqual(49);

    await scrollToBottom(page);

    // The last row in the form (toggle + label) should remain above the save bar.
    const textarea = page.locator('textarea[placeholder="Enter ad post text..."]:visible, textarea[placeholder="Введите текст рекламного поста..."]:visible');
    const form = page.locator('.creative-editor-mobile, .creative-editor-desktop').filter({ has: textarea }).first();
    const disablePreviewLabel = form.getByText(/^(Disable link previews|Отключить превью ссылок)$/);
    await expect(disablePreviewLabel).toBeVisible();

    const labelBox = await disablePreviewLabel.boundingBox();
    const saveBox = await save.boundingBox();
    expect(labelBox).toBeTruthy();
    expect(saveBox).toBeTruthy();
    if (labelBox && saveBox) {
      expect(labelBox.y + labelBox.height).toBeLessThanOrEqual(saveBox.y - 8);
    }
  });
});

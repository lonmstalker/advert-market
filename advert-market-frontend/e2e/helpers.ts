import type { Page } from '@playwright/test';

export async function completeOnboarding(page: Page) {
  await page.goto('/');
  await page.getByRole('button', { name: 'Get Started' }).click();
  await page.getByRole('button', { name: /advertiser/i }).click();
  await page.getByRole('button', { name: 'Continue' }).click();
  await page.getByText('Skip').click();
  await page.waitForURL('**/catalog');
}

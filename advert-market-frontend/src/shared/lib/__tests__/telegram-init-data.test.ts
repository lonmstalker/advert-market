import { describe, expect, it, vi } from 'vitest';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(),
}));

const { retrieveRawInitData } = await import('@telegram-apps/sdk-react');
const { getTelegramInitData } = await import('../telegram-init-data');

describe('getTelegramInitData', () => {
  it('returns SDK initData when available', () => {
    vi.mocked(retrieveRawInitData).mockReturnValueOnce('sdk-init-data');

    expect(getTelegramInitData()).toBe('sdk-init-data');
  });

  it('falls back to window.Telegram.WebApp.initData when SDK throws', () => {
    vi.mocked(retrieveRawInitData).mockImplementationOnce(() => {
      throw new Error('bridge not ready');
    });

    // @ts-expect-error test setup: Telegram WebApp is injected by Telegram client
    window.Telegram = { WebApp: { initData: 'fallback-init-data' } };

    expect(getTelegramInitData()).toBe('fallback-init-data');
  });

  it('returns empty string when neither SDK nor window provides initData', () => {
    vi.mocked(retrieveRawInitData).mockReturnValueOnce('');

    // @ts-expect-error test setup
    window.Telegram = undefined;

    expect(getTelegramInitData()).toBe('');
  });
});

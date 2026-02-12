import { renderHook } from '@testing-library/react';
import { useTelegram } from '@/shared/hooks/use-telegram';

describe('useTelegram', () => {
  beforeEach(() => {
    // biome-ignore lint/suspicious/noExplicitAny: mocking window.Telegram for test
    delete (window as any).Telegram;
  });

  it('returns defaults when Telegram is not available', () => {
    const { result } = renderHook(() => useTelegram());

    expect(result.current.initDataRaw).toBeUndefined();
    expect(result.current.user).toBeUndefined();
    expect(result.current.colorScheme).toBe('light');
    expect(result.current.platform).toBe('unknown');
    expect(result.current.isExpanded).toBe(false);
    expect(typeof result.current.expand).toBe('function');
    expect(typeof result.current.close).toBe('function');
    expect(typeof result.current.ready).toBe('function');
  });

  it('returns user data when WebApp is available', () => {
    // biome-ignore lint/suspicious/noExplicitAny: mocking window.Telegram for test
    (window as any).Telegram = {
      WebApp: {
        initData: 'raw-init-data',
        initDataUnsafe: {
          user: {
            id: 12345,
            first_name: 'John',
            last_name: 'Doe',
            username: 'johndoe',
            language_code: 'en',
          },
        },
        colorScheme: 'light',
        platform: 'android',
        isExpanded: true,
        expand: vi.fn(),
        close: vi.fn(),
        ready: vi.fn(),
      },
    };

    const { result } = renderHook(() => useTelegram());

    expect(result.current.initDataRaw).toBe('raw-init-data');
    expect(result.current.user).toEqual({
      id: 12345,
      firstName: 'John',
      lastName: 'Doe',
      username: 'johndoe',
      languageCode: 'en',
    });
    expect(result.current.platform).toBe('android');
    expect(result.current.isExpanded).toBe(true);
  });

  it('returns colorScheme from WebApp', () => {
    // biome-ignore lint/suspicious/noExplicitAny: mocking window.Telegram for test
    (window as any).Telegram = {
      WebApp: {
        initData: '',
        initDataUnsafe: {},
        colorScheme: 'dark',
        platform: 'ios',
        isExpanded: false,
        expand: vi.fn(),
        close: vi.fn(),
        ready: vi.fn(),
      },
    };

    const { result } = renderHook(() => useTelegram());

    expect(result.current.colorScheme).toBe('dark');
  });
});

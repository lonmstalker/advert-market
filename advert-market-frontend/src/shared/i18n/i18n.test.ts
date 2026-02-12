import { installTelegramMock, removeTelegramMock } from '@/test/mocks/telegram';
import { detectLanguage } from './i18n';

describe('detectLanguage', () => {
  afterEach(() => {
    removeTelegramMock();
    vi.restoreAllMocks();
    vi.unstubAllEnvs();
  });

  it('returns forced locale "en" from VITE_FORCE_LOCALE', () => {
    vi.stubEnv('VITE_FORCE_LOCALE', 'en');
    installTelegramMock({
      initDataUnsafe: { user: { language_code: 'ru' } },
    });
    expect(detectLanguage()).toBe('en');
  });

  it('returns forced locale "ru" from VITE_FORCE_LOCALE', () => {
    vi.stubEnv('VITE_FORCE_LOCALE', 'ru');
    vi.spyOn(navigator, 'language', 'get').mockReturnValue('en-US');
    expect(detectLanguage()).toBe('ru');
  });

  it('ignores invalid VITE_FORCE_LOCALE value', () => {
    vi.stubEnv('VITE_FORCE_LOCALE', 'de');
    vi.spyOn(navigator, 'language', 'get').mockReturnValue('en-US');
    expect(detectLanguage()).toBe('ru');
  });

  it('returns "ru" for Telegram user with language_code "ru"', () => {
    installTelegramMock({
      initDataUnsafe: { user: { language_code: 'ru' } },
    });
    expect(detectLanguage()).toBe('ru');
  });

  it('returns "en" for Telegram user with language_code "en"', () => {
    installTelegramMock({
      initDataUnsafe: { user: { language_code: 'en' } },
    });
    expect(detectLanguage()).toBe('en');
  });

  it('returns "ru" from browser language "ru-RU" without Telegram', () => {
    vi.spyOn(navigator, 'language', 'get').mockReturnValue('ru-RU');
    expect(detectLanguage()).toBe('ru');
  });

  it('returns "ru" as default for browser language "en-US" without Telegram', () => {
    vi.spyOn(navigator, 'language', 'get').mockReturnValue('en-US');
    expect(detectLanguage()).toBe('ru');
  });

  it('returns "ru" when no Telegram and empty browser language', () => {
    vi.spyOn(navigator, 'language', 'get').mockReturnValue('');
    expect(detectLanguage()).toBe('ru');
  });
});

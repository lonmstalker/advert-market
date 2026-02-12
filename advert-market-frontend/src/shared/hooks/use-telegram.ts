import { useMemo } from 'react';

type TelegramUser = {
  id: number;
  firstName: string;
  lastName?: string;
  username?: string;
  languageCode?: string;
};

type TelegramContext = {
  initDataRaw: string | undefined;
  user: TelegramUser | undefined;
  colorScheme: 'light' | 'dark';
  platform: string;
  isExpanded: boolean;
  expand: () => void;
  close: () => void;
  ready: () => void;
};

function getWebApp() {
  return window.Telegram?.WebApp;
}

export function useTelegram(): TelegramContext {
  return useMemo(() => {
    try {
      const webApp = getWebApp();
      if (!webApp) return createDefaults();

      const tgUser = webApp.initDataUnsafe?.user;
      const user: TelegramUser | undefined = tgUser
        ? {
            id: tgUser.id,
            firstName: tgUser.first_name,
            lastName: tgUser.last_name,
            username: tgUser.username,
            languageCode: tgUser.language_code,
          }
        : undefined;

      return {
        initDataRaw: webApp.initData || undefined,
        user,
        colorScheme: webApp.colorScheme === 'dark' ? 'dark' : 'light',
        platform: webApp.platform || 'unknown',
        isExpanded: webApp.isExpanded ?? false,
        expand: () => webApp.expand?.(),
        close: () => webApp.close?.(),
        ready: () => webApp.ready?.(),
      };
    } catch {
      return createDefaults();
    }
  }, []);
}

function createDefaults(): TelegramContext {
  const noop = () => {};
  return {
    initDataRaw: undefined,
    user: undefined,
    colorScheme: 'light',
    platform: 'unknown',
    isExpanded: false,
    expand: noop,
    close: noop,
    ready: noop,
  };
}

// Minimal window.Telegram type for edge cases outside SDK context.
// Prefer @telegram-apps/sdk-react hooks over direct window.Telegram access.

type TelegramWebAppUser = {
  id: number;
  first_name: string;
  last_name?: string;
  username?: string;
  language_code?: string;
};

type TelegramWebAppInitDataUnsafe = {
  start_param?: string;
  user?: TelegramWebAppUser;
};

type TelegramWebAppBackButton = {
  show(): void;
  hide(): void;
  onClick(callback: () => void): void;
  offClick(callback: () => void): void;
};

interface Window {
  Telegram?: {
    WebApp: {
      initData: string;
      initDataUnsafe?: TelegramWebAppInitDataUnsafe;
      colorScheme: 'light' | 'dark';
      platform?: string;
      isExpanded?: boolean;
      BackButton?: TelegramWebAppBackButton;
      onEvent?(eventType: string, eventHandler: (...args: unknown[]) => void): void;
      offEvent?(eventType: string, eventHandler: (...args: unknown[]) => void): void;
      expand?(): void;
      close?(): void;
      ready?(): void;
    };
  };
}

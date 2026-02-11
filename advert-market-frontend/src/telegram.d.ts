// Minimal window.Telegram type for edge cases outside SDK context.
// Prefer @telegram-apps/sdk-react hooks over direct window.Telegram access.
interface Window {
  Telegram?: {
    WebApp: {
      initData: string;
      colorScheme: 'light' | 'dark';
      expand(): void;
      close(): void;
      ready(): void;
    };
  };
}
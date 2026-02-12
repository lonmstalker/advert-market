export type MockBackButton = {
  show: ReturnType<typeof vi.fn>;
  hide: ReturnType<typeof vi.fn>;
  onClick: ReturnType<typeof vi.fn>;
  offClick: ReturnType<typeof vi.fn>;
};

export type MockTelegramWebApp = {
  initData: string;
  initDataUnsafe: {
    user?: { language_code?: string };
    start_param?: string;
  };
  colorScheme: 'light' | 'dark';
  BackButton: MockBackButton;
  expand: ReturnType<typeof vi.fn>;
  close: ReturnType<typeof vi.fn>;
  ready: ReturnType<typeof vi.fn>;
};

export function installTelegramMock(overrides?: Partial<MockTelegramWebApp>): MockTelegramWebApp {
  const backButton: MockBackButton = {
    show: vi.fn(),
    hide: vi.fn(),
    onClick: vi.fn(),
    offClick: vi.fn(),
  };

  const webApp: MockTelegramWebApp = {
    initData: 'mock-init-data',
    initDataUnsafe: {},
    colorScheme: 'light',
    BackButton: backButton,
    expand: vi.fn(),
    close: vi.fn(),
    ready: vi.fn(),
    ...overrides,
  };

  Object.defineProperty(window, 'Telegram', {
    value: { WebApp: webApp },
    writable: true,
    configurable: true,
  });

  return webApp;
}

export function removeTelegramMock(): void {
  Object.defineProperty(window, 'Telegram', {
    value: undefined,
    writable: true,
    configurable: true,
  });
}

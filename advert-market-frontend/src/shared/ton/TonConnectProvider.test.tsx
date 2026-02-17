import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

type CapturedProps = {
  manifestUrl?: string;
  actionsConfiguration?: { twaReturnUrl?: string };
};

const captured: CapturedProps = {};

vi.mock('@tonconnect/ui-react', () => ({
  TonConnectUIProvider: ({
    manifestUrl,
    actionsConfiguration,
    children,
  }: {
    manifestUrl: string;
    actionsConfiguration?: { twaReturnUrl?: string };
    children: React.ReactNode;
  }) => {
    captured.manifestUrl = manifestUrl;
    captured.actionsConfiguration = actionsConfiguration;
    return <div data-testid="ton-provider">{children}</div>;
  },
}));

vi.mock('react-router', () => ({
  Outlet: () => <div data-testid="mock-outlet" />,
}));

describe('TonConnectProvider', () => {
  beforeEach(() => {
    vi.unstubAllEnvs();
    vi.resetModules();
    captured.manifestUrl = undefined;
    captured.actionsConfiguration = undefined;
  });

  it('uses defaults from current origin and default bot return URL', async () => {
    const { TonConnectProvider } = await import('./TonConnectProvider');

    render(<TonConnectProvider />);

    expect(captured.manifestUrl).toBe(`${window.location.origin}/tonconnect-manifest.json`);
    expect(captured.actionsConfiguration).toEqual({
      twaReturnUrl: 'https://t.me/AdvertMarketBot/app',
    });
    expect(screen.getByTestId('mock-outlet')).toBeInTheDocument();
  });

  it('uses env overrides for manifest and TWA return URL', async () => {
    vi.stubEnv('VITE_TON_MANIFEST_URL', 'https://teleinsight.in/tonconnect-manifest.json');
    vi.stubEnv('VITE_TWA_RETURN_URL', 'https://t.me/TeleInsightBot/app');

    const { TonConnectProvider } = await import('./TonConnectProvider');

    render(<TonConnectProvider />);

    expect(captured.manifestUrl).toBe('https://teleinsight.in/tonconnect-manifest.json');
    expect(captured.actionsConfiguration).toEqual({
      twaReturnUrl: 'https://t.me/TeleInsightBot/app',
    });
  });
});

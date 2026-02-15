import { TonConnectUIProvider } from '@tonconnect/ui-react';
import { Outlet } from 'react-router';

const TON_MANIFEST_URL = import.meta.env.VITE_TON_MANIFEST_URL ?? `${window.location.origin}/tonconnect-manifest.json`;

export function TonConnectProvider() {
  return (
    <TonConnectUIProvider
      manifestUrl={TON_MANIFEST_URL}
      actionsConfiguration={{ twaReturnUrl: 'https://t.me/AdvertMarketBot/app' }}
    >
      <Outlet />
    </TonConnectUIProvider>
  );
}

import { TonConnectUIProvider } from '@tonconnect/ui-react';
import { Outlet } from 'react-router';

const TON_MANIFEST_URL = import.meta.env.VITE_TON_MANIFEST_URL ?? `${window.location.origin}/tonconnect-manifest.json`;
const TON_TWA_RETURN_URL = import.meta.env.VITE_TWA_RETURN_URL ?? 'https://t.me/AdvertMarketBot/app';

export function TonConnectProvider() {
  return (
    <TonConnectUIProvider manifestUrl={TON_MANIFEST_URL} actionsConfiguration={{ twaReturnUrl: TON_TWA_RETURN_URL }}>
      <Outlet />
    </TonConnectUIProvider>
  );
}

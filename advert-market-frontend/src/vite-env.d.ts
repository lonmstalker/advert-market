/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_TON_NETWORK: 'mainnet' | 'testnet';
  readonly VITE_TON_MANIFEST_URL?: string;
  readonly VITE_TON_DEPOSIT_POLL_INTERVAL_MS?: string;
  readonly VITE_MOCK_API?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare const __DEV__: boolean;

/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_TON_NETWORK: 'mainnet' | 'testnet';
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

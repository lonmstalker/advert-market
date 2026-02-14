import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import mkcert from 'vite-plugin-mkcert';
import { resolve } from 'path';

export default defineConfig(({ mode }) => {
  const isE2E = mode === 'e2e';

  return {
    // Playwright's webServer readiness check does not trust mkcert by default.
    // We disable mkcert for e2e to keep local runs deterministic.
    plugins: isE2E ? [react()] : [react(), mkcert()],
    resolve: {
      alias: {
        '@': resolve(process.cwd(), 'src'),
      },
    },
    server: {
      port: 3000,
      https: isE2E ? false : {},
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    build: {
      target: 'es2023',
      rollupOptions: {
        output: {
          manualChunks: {
            react: ['react', 'react-dom', 'react-router'],
            query: ['@tanstack/react-query'],
            i18n: ['i18next', 'react-i18next'],
            tgui: ['@telegram-tools/ui-kit'],
            ton: ['@tonconnect/ui-react'],
            tma: ['@telegram-apps/sdk-react'],
          },
        },
      },
    },
    define: {
      __DEV__: mode === 'development',
    },
  };
});

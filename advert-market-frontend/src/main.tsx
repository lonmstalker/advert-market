import { init, miniApp, viewport } from '@telegram-apps/sdk-react';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@/app/App';

import '@telegram-tools/ui-kit/dist/index.css';
import '@/app/global.css';

// Eruda dev console for mobile debugging (dev only, lazy-loaded)
// Disabled in E2E runs because it can intercept pointer events (especially in WebKit).
if (import.meta.env.DEV && import.meta.env.MODE !== 'e2e') {
  import('eruda').then(({ default: eruda }) => eruda.init());
}

// Initialize Telegram Mini Apps SDK — sets up event listeners and bridge
try {
  init();
  miniApp.ready();
  viewport.expand();
} catch {
  // Outside Telegram — SDK init fails, app continues with limited functionality
}

async function enableMocking() {
  if (import.meta.env.VITE_MOCK_API !== 'true') return;

  const { worker } = await import('@/test/mocks/browser');
  return worker.start({ onUnhandledRequest: 'bypass' });
}

async function startApp() {
  // TODO: Replace with real API — remove enableMocking when backend is ready
  await enableMocking();

  const { initI18n } = await import('@/shared/i18n');
  await initI18n();

  const root = document.getElementById('root');
  if (!root) throw new Error('Root element not found');

  createRoot(root).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
}

startApp();

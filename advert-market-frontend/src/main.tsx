import { init, miniApp, themeParams, viewport } from '@telegram-apps/sdk-react';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@/app/App';

import '@telegram-tools/ui-kit/dist/index.css';
import '@/app/global.css';

let webViewportFallbackBound = false;

function bindWebViewportCssVarsFallback() {
  if (webViewportFallbackBound) return;
  webViewportFallbackBound = true;

  // Outside Telegram, `100vh` is not stable on iOS. Use `innerHeight` as a pragmatic fallback
  // so fixed-bottom bars and onboarding CTAs don't slip under the browser chrome.
  const root = document.documentElement;

  const update = () => {
    const stable = window.innerHeight;
    const dynamic = window.visualViewport?.height ?? stable;
    root.style.setProperty('--tg-viewport-stable-height', `${stable}px`);
    root.style.setProperty('--tg-viewport-height', `${dynamic}px`);
  };

  update();
  window.addEventListener('resize', update);
  window.visualViewport?.addEventListener('resize', update);
}

// Eruda dev console for mobile debugging (dev only, lazy-loaded)
// Disabled in E2E runs because it can intercept pointer events (especially in WebKit).
if (import.meta.env.DEV && import.meta.env.MODE !== 'e2e') {
  import('eruda').then(({ default: eruda }) => eruda.init());
}

// Initialize Telegram Mini Apps SDK — sets up event listeners and bridge.
// Must degrade gracefully outside Telegram (E2E / dev web).
async function initTelegramSdk() {
  try {
    init();
  } catch {
    bindWebViewportCssVarsFallback();
    return;
  }

  // Viewport: stable height + safe/content insets via CSS vars.
  try {
    if (viewport.mount.isAvailable() && !viewport.isMounted()) {
      await viewport.mount();
    }
  } catch {
    /* no-op */
  }
  try {
    if (viewport.bindCssVars.isAvailable() && !viewport.isCssVarsBound()) {
      viewport.bindCssVars();
    }
  } catch {
    /* no-op */
  }

  // Outside Telegram, viewport CSS vars will not bind. Keep layout stable with a browser fallback.
  try {
    if (!viewport.isCssVarsBound()) {
      bindWebViewportCssVarsFallback();
    }
  } catch {
    bindWebViewportCssVarsFallback();
  }

  // Theme params: bind `--tg-theme-*` vars so UI Kit tokens follow Telegram theme.
  try {
    if (themeParams.mountSync.isAvailable() && !themeParams.isMounted()) {
      themeParams.mountSync();
    }
  } catch {
    /* no-op */
  }
  try {
    if (themeParams.bindCssVars.isAvailable() && !themeParams.isCssVarsBound()) {
      themeParams.bindCssVars();
    }
  } catch {
    /* no-op */
  }

  try {
    if (miniApp.ready.isAvailable()) {
      miniApp.ready();
    }
  } catch {
    /* no-op */
  }

  try {
    if (viewport.expand.isAvailable()) {
      viewport.expand();
    }
  } catch {
    /* no-op */
  }
}

void initTelegramSdk();

async function enableMocking() {
  if (import.meta.env.VITE_MOCK_API !== 'true') return;

  const { worker } = await import('@/test/mocks/browser');
  return worker.start({ onUnhandledRequest: 'bypass' });
}

async function startApp() {
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

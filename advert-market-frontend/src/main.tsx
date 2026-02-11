import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { init } from '@telegram-apps/sdk-react';
import { App } from '@/app/App';

import '@telegram-tools/ui-kit/dist/index.css';
import '@/app/global.css';

// Eruda dev console for mobile debugging (dev only, lazy-loaded)
if (import.meta.env.DEV) {
  import('eruda').then(({ default: eruda }) => eruda.init());
}

// Initialize Telegram Mini Apps SDK — sets up event listeners and bridge
init();

const root = document.getElementById('root');
if (!root) throw new Error('Root element not found');

createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@/app/App';

import '@telegram-tools/ui-kit/dist/index.css';
import '@/app/global.css';

window.Telegram?.WebApp.expand();
window.Telegram?.WebApp.ready();

const root = document.getElementById('root');
if (!root) throw new Error('Root element not found');

createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
);

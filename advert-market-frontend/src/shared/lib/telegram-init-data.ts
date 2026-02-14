import { retrieveRawInitData } from '@telegram-apps/sdk-react';

/**
 * Best-effort retrieval of Telegram WebApp initData.
 *
 * The SDK can throw if the bridge is not available or not initialized yet.
 * Telegram also exposes initData directly via window.Telegram.WebApp.initData.
 */
export function getTelegramInitData(): string {
  try {
    const raw = retrieveRawInitData() ?? '';
    if (raw) return raw;
  } catch {
    // Fall back to direct Telegram WebApp API.
  }

  try {
    const raw = window.Telegram?.WebApp?.initData;
    return typeof raw === 'string' ? raw : '';
  } catch {
    return '';
  }
}

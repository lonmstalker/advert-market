import { retrieveRawInitData } from '@telegram-apps/sdk-react';

const INIT_DATA_CACHE_KEY = 'telegram_init_data_cache';

/**
 * Best-effort retrieval of Telegram WebApp initData.
 *
 * The SDK can throw if the bridge is not available or not initialized yet.
 * Telegram also exposes initData directly via window.Telegram.WebApp.initData.
 */
export function getTelegramInitData(): string {
  const cache = (value: string): string => {
    if (!value) return '';
    try {
      sessionStorage.setItem(INIT_DATA_CACHE_KEY, value);
    } catch {
      // Ignore storage failures in restricted environments.
    }
    return value;
  };

  try {
    const raw = retrieveRawInitData() ?? '';
    if (raw) return cache(raw);
  } catch {
    // Fall back to direct Telegram WebApp API.
  }

  try {
    const raw = window.Telegram?.WebApp?.initData;
    if (typeof raw === 'string' && raw) {
      return cache(raw);
    }
  } catch {
    // Fall back to session cache below.
  }

  try {
    const cached = sessionStorage.getItem(INIT_DATA_CACHE_KEY);
    return cached ?? '';
  } catch {
    return '';
  }
}

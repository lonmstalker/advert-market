import type { Resource } from 'i18next';
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

export function detectLanguage(): string {
  // 0. Forced locale for E2E tests
  const forced = import.meta.env.VITE_FORCE_LOCALE;
  if (forced === 'en' || forced === 'ru') return forced;

  // 1. Telegram Mini App language
  try {
    const tg = window.Telegram?.WebApp;
    if (tg?.initDataUnsafe?.user?.language_code) {
      return tg.initDataUnsafe.user.language_code.startsWith('ru') ? 'ru' : 'en';
    }
  } catch {
    // Outside Telegram — fallback below
  }

  // 2. Browser language
  const browserLang = navigator.language;
  if (browserLang.startsWith('ru')) return 'ru';

  // 3. Default
  return 'ru';
}

const localeLoaders = {
  ru: () => import('./locales/ru.json'),
  en: () => import('./locales/en.json'),
} as const;

export async function initI18n() {
  const lng = detectLanguage() as keyof typeof localeLoaders;

  const resources: Resource = {};

  const primary = await localeLoaders[lng]();
  resources[lng] = { translation: primary.default };

  if (lng !== 'ru') {
    const fallback = await localeLoaders.ru();
    resources.ru = { translation: fallback.default };
  }

  await i18n.use(initReactI18next).init({
    resources,
    lng,
    fallbackLng: 'ru',
    interpolation: {
      escapeValue: false,
    },
  });
}

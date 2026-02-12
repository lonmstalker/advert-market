import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import en from './locales/en.json';
import ru from './locales/ru.json';

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

export async function initI18n() {
  await i18n.use(initReactI18next).init({
    resources: {
      ru: { translation: ru },
      en: { translation: en },
    },
    lng: detectLanguage(),
    fallbackLng: 'ru',
    interpolation: {
      escapeValue: false,
    },
  });
}

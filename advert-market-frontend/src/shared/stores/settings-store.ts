import { create } from 'zustand';
import type { CurrencyMode, NotificationSettings, UserProfile } from '@/shared/api/auth';

type SettingsState = {
  languageCode: string;
  displayCurrency: string;
  currencyMode: CurrencyMode;
  notificationSettings: NotificationSettings;
  isLoaded: boolean;

  setFromProfile: (profile: UserProfile) => void;
  setLanguageCode: (languageCode: string) => void;
  setDisplayCurrency: (currency: string) => void;
  setCurrencyMode: (mode: CurrencyMode) => void;
  setNotificationSetting: (group: keyof NotificationSettings, key: string, value: boolean) => void;
};

const DEFAULT_NOTIFICATIONS: NotificationSettings = {
  deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
  financial: { deposits: true, payouts: true, escrow: true },
  disputes: { opened: true, resolved: true },
};

export const useSettingsStore = create<SettingsState>((set) => ({
  languageCode: 'en',
  displayCurrency: 'USD',
  currencyMode: 'AUTO',
  notificationSettings: DEFAULT_NOTIFICATIONS,
  isLoaded: false,

  setFromProfile: (profile) =>
    set({
      languageCode: profile.languageCode,
      displayCurrency: profile.displayCurrency,
      currencyMode: profile.currencyMode,
      notificationSettings: profile.notificationSettings,
      isLoaded: true,
    }),

  setLanguageCode: (languageCode) => set({ languageCode }),
  setDisplayCurrency: (currency) => set({ displayCurrency: currency }),
  setCurrencyMode: (mode) => set({ currencyMode: mode }),

  setNotificationSetting: (group, key, value) =>
    set((state) => ({
      notificationSettings: {
        ...state.notificationSettings,
        [group]: {
          ...state.notificationSettings[group],
          [key]: value,
        },
      },
    })),
}));

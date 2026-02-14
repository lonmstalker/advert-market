import { create } from 'zustand';
import type { NotificationSettings, UserProfile } from '@/shared/api/auth';

type SettingsState = {
  displayCurrency: string;
  notificationSettings: NotificationSettings;
  isLoaded: boolean;

  setFromProfile: (profile: UserProfile) => void;
  setDisplayCurrency: (currency: string) => void;
  setNotificationSetting: (group: keyof NotificationSettings, key: string, value: boolean) => void;
};

const DEFAULT_NOTIFICATIONS: NotificationSettings = {
  deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
  financial: { deposits: true, payouts: true, escrow: true },
  disputes: { opened: true, resolved: true },
};

export const useSettingsStore = create<SettingsState>((set) => ({
  displayCurrency: 'USD',
  notificationSettings: DEFAULT_NOTIFICATIONS,
  isLoaded: false,

  setFromProfile: (profile) =>
    set({
      displayCurrency: profile.displayCurrency,
      notificationSettings: profile.notificationSettings,
      isLoaded: true,
    }),

  setDisplayCurrency: (currency) => set({ displayCurrency: currency }),

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

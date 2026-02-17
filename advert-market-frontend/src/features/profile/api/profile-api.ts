import type { CurrencyMode, NotificationSettings } from '@/shared/api';
import type { UserProfile } from '@/shared/api/auth';
import { userProfileSchema } from '@/shared/api/auth';
import { api } from '@/shared/api/client';

export function updateLanguage(languageCode: string): Promise<UserProfile> {
  return api.put<UserProfile>('/profile/language', { languageCode }, { schema: userProfileSchema });
}

export function updateWallet(tonAddress: string): Promise<UserProfile> {
  return api.put<UserProfile>('/profile/wallet', { tonAddress }, { schema: userProfileSchema });
}

type UpdateSettingsRequest = {
  displayCurrency?: string;
  currencyMode?: CurrencyMode;
  notificationSettings?: NotificationSettings;
};

export function updateSettings(settings: UpdateSettingsRequest): Promise<UserProfile> {
  return api.put<UserProfile>('/profile/settings', settings, { schema: userProfileSchema });
}

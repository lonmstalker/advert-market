import type { CurrencyMode, NotificationSettings, UserProfile } from './auth';
import { userProfileSchema } from './auth';
import { api } from './client';

export function updateLanguage(languageCode: string): Promise<UserProfile> {
  return api.put<UserProfile>('/profile/language', { languageCode }, { schema: userProfileSchema });
}

type UpdateSettingsRequest = {
  displayCurrency?: string;
  currencyMode?: CurrencyMode;
  notificationSettings?: NotificationSettings;
};

export function updateSettings(settings: UpdateSettingsRequest): Promise<UserProfile> {
  return api.put<UserProfile>('/profile/settings', settings, { schema: userProfileSchema });
}

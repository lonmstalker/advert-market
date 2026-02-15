import type { UserProfile } from './auth';
import { userProfileSchema } from './auth';
import { api } from './client';

export function updateLanguage(languageCode: string): Promise<UserProfile> {
  return api.put<UserProfile>('/profile/language', { languageCode }, { schema: userProfileSchema });
}

import type { UserProfile } from '@/shared/api/auth';

export function preserveLanguageOnSettingsUpdate(
  updatedProfile: UserProfile,
  currentLanguageCode: string,
): UserProfile {
  if (!currentLanguageCode || updatedProfile.languageCode === currentLanguageCode) {
    return updatedProfile;
  }

  return {
    ...updatedProfile,
    languageCode: currentLanguageCode,
  };
}

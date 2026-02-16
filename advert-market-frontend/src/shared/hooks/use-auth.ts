import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { authKeys, profileKeys } from '@/shared/api';
import type { UserProfile } from '@/shared/api/auth';
import { fetchProfile, login } from '@/shared/api/auth';
import { getTelegramInitData } from '@/shared/lib/telegram-init-data';
import { useSettingsStore } from '@/shared/stores/settings-store';

function normalizeLanguage(code: string): string {
  return (code.split('-')[0] ?? code).toLowerCase();
}

export function useAuth() {
  const { i18n } = useTranslation();

  const sessionQuery = useQuery({
    queryKey: authKeys.session,
    queryFn: async () => {
      const existing = sessionStorage.getItem('access_token');
      if (existing) return { accessToken: existing };

      const initData = getTelegramInitData();

      const response = await login(initData);
      return { accessToken: response.accessToken };
    },
    staleTime: Number.POSITIVE_INFINITY,
    retry: 2,
  });

  const profileQuery = useQuery<UserProfile>({
    queryKey: profileKeys.me,
    queryFn: fetchProfile,
    enabled: !!sessionQuery.data?.accessToken,
    staleTime: 5 * 60 * 1000,
  });

  const setFromProfile = useSettingsStore((s) => s.setFromProfile);

  useEffect(() => {
    if (!profileQuery.data) return;

    setFromProfile(profileQuery.data);

    const nextLanguage = normalizeLanguage(profileQuery.data.languageCode);
    const currentLanguage = normalizeLanguage(i18n.language);
    if (nextLanguage !== currentLanguage) {
      i18n.changeLanguage(nextLanguage);
    }
  }, [profileQuery.data, i18n, setFromProfile]);

  return {
    isAuthenticated: !!sessionQuery.data?.accessToken,
    isLoading: sessionQuery.isLoading || (sessionQuery.isSuccess && profileQuery.isLoading),
    authError: sessionQuery.isError ? (sessionQuery.error as Error) : null,
    retryAuth: () => sessionQuery.refetch(),
    profile: profileQuery.data ?? null,
    invalidateProfile: () => profileQuery.refetch(),
  };
}

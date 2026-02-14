import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { authKeys, profileKeys } from '@/shared/api';
import type { UserProfile } from '@/shared/api/auth';
import { fetchProfile, login } from '@/shared/api/auth';
import { getTelegramInitData } from '@/shared/lib/telegram-init-data';
import { useSettingsStore } from '@/shared/stores/settings-store';

export function useAuth() {
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
  const isLoaded = useSettingsStore((s) => s.isLoaded);

  useEffect(() => {
    if (profileQuery.data && !isLoaded) {
      setFromProfile(profileQuery.data);
    }
  }, [profileQuery.data, isLoaded, setFromProfile]);

  return {
    isAuthenticated: !!sessionQuery.data?.accessToken,
    isLoading: sessionQuery.isLoading || (sessionQuery.isSuccess && profileQuery.isLoading),
    authError: sessionQuery.isError ? (sessionQuery.error as Error) : null,
    retryAuth: () => sessionQuery.refetch(),
    profile: profileQuery.data ?? null,
    invalidateProfile: () => profileQuery.refetch(),
  };
}

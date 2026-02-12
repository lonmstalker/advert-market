import { useQuery } from '@tanstack/react-query';
import { retrieveRawInitData } from '@telegram-apps/sdk-react';
import { authKeys } from '@/shared/api';
import { fetchProfile, login } from '../api/auth-api';
import type { UserProfile } from '../types/auth';

const profileKeys = {
  current: ['auth', 'profile'] as const,
};

export function useAuth() {
  const sessionQuery = useQuery({
    queryKey: authKeys.session,
    queryFn: async () => {
      const existing = sessionStorage.getItem('access_token');
      if (existing) return { accessToken: existing };

      let initData = '';
      try {
        initData = retrieveRawInitData() ?? '';
      } catch {
        // Outside Telegram — use empty initData (MSW will handle it)
      }

      const response = await login(initData);
      return { accessToken: response.accessToken };
    },
    staleTime: Number.POSITIVE_INFINITY,
    retry: 2,
  });

  const profileQuery = useQuery<UserProfile>({
    queryKey: profileKeys.current,
    queryFn: fetchProfile,
    enabled: !!sessionQuery.data?.accessToken,
    staleTime: 5 * 60 * 1000,
  });

  return {
    isAuthenticated: !!sessionQuery.data?.accessToken,
    isLoading: sessionQuery.isLoading || (sessionQuery.isSuccess && profileQuery.isLoading),
    profile: profileQuery.data ?? null,
    invalidateProfile: () => profileQuery.refetch(),
  };
}

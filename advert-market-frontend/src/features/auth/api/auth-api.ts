import { type AuthResponse, api } from '@/shared/api';
import { authResponseSchema } from '@/shared/api/types';
import type { UserProfile } from '../types/auth';
import { userProfileSchema } from '../types/auth';

export async function login(initData: string): Promise<AuthResponse> {
  const response = await api.post<AuthResponse>('/auth/login', { initData }, { schema: authResponseSchema });
  sessionStorage.setItem('access_token', response.accessToken);
  return response;
}

export async function fetchProfile(): Promise<UserProfile> {
  return api.get<UserProfile>('/profile', { schema: userProfileSchema });
}

export async function completeOnboarding(interests: string[]): Promise<UserProfile> {
  return api.put<UserProfile>('/profile/onboarding', { interests }, { schema: userProfileSchema });
}

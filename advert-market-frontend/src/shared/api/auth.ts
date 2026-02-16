import { z } from 'zod/v4';
import { api } from './client';
import type { AuthResponse } from './types';
import { authResponseSchema } from './types';

const notificationSettingsSchema = z.object({
  deals: z.object({
    newOffers: z.boolean(),
    acceptReject: z.boolean(),
    deliveryStatus: z.boolean(),
  }),
  financial: z.object({
    deposits: z.boolean(),
    payouts: z.boolean(),
    escrow: z.boolean(),
  }),
  disputes: z.object({
    opened: z.boolean(),
    resolved: z.boolean(),
  }),
});

export type NotificationSettings = z.infer<typeof notificationSettingsSchema>;
export const currencyModeSchema = z.enum(['AUTO', 'MANUAL']);
export type CurrencyMode = z.infer<typeof currencyModeSchema>;

export const userProfileSchema = z
  .object({
    id: z.number(),
    telegramId: z.number().optional(),
    username: z.string(),
    displayName: z.string(),
    languageCode: z.string(),
    displayCurrency: z.string(),
    currencyMode: currencyModeSchema,
    notificationSettings: notificationSettingsSchema,
    onboardingCompleted: z.boolean(),
    interests: z.array(z.string()),
    createdAt: z.string(),
  })
  .transform((profile) => ({
    ...profile,
    telegramId: profile.telegramId ?? profile.id,
  }));

export type UserProfile = z.infer<typeof userProfileSchema>;

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

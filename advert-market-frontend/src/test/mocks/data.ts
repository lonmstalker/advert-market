import type { AuthResponse } from '@/shared/api';

export const mockUser = {
  id: 1,
  telegramId: 123456789,
  username: 'testuser',
  displayName: 'Test User',
  languageCode: 'ru',
  onboardingCompleted: false,
  interests: [] as string[],
  createdAt: '2026-01-15T10:00:00Z',
};

export const mockAuthResponse: AuthResponse = {
  accessToken: 'mock-jwt-token-for-development',
  expiresIn: 86400,
  user: {
    id: mockUser.id,
    username: mockUser.username,
    displayName: mockUser.displayName,
  },
};

export const mockProfile = {
  id: mockUser.id,
  telegramId: mockUser.telegramId,
  username: mockUser.username,
  displayName: mockUser.displayName,
  languageCode: mockUser.languageCode,
  onboardingCompleted: mockUser.onboardingCompleted,
  interests: mockUser.interests,
  createdAt: mockUser.createdAt,
};

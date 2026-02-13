import { describe, expect, it } from 'vitest';
import { userProfileSchema } from './auth';

describe('userProfileSchema', () => {
  it('fills telegramId from id when telegramId is missing', () => {
    const profile = userProfileSchema.parse({
      id: 42,
      username: 'alice',
      displayName: 'Alice',
      languageCode: 'en',
      onboardingCompleted: true,
      interests: ['crypto'],
      createdAt: '2026-02-13T00:00:00Z',
    });

    expect(profile.telegramId).toBe(42);
  });
});

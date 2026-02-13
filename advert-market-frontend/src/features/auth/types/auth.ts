import { z } from 'zod/v4';

export const userProfileSchema = z
  .object({
    id: z.number(),
    telegramId: z.number().optional(),
    username: z.string(),
    displayName: z.string(),
    languageCode: z.string(),
    onboardingCompleted: z.boolean(),
    interests: z.array(z.string()),
    createdAt: z.string(),
  })
  .transform((profile) => ({
    ...profile,
    telegramId: profile.telegramId ?? profile.id,
  }));

export type UserProfile = z.infer<typeof userProfileSchema>;

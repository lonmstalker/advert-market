import { z } from 'zod/v4';

export const userProfileSchema = z.object({
  id: z.number(),
  telegramId: z.number(),
  username: z.string(),
  displayName: z.string(),
  languageCode: z.string(),
  onboardingCompleted: z.boolean(),
  interests: z.array(z.string()),
  createdAt: z.string(),
});

export type UserProfile = z.infer<typeof userProfileSchema>;

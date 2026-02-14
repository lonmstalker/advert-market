import { z } from 'zod/v4';

export type { InlineButton, MediaItem, MediaType, TextEntity } from '@/shared/types/text-entity';
// Re-export shared types for convenience within the feature
export { TextEntityType } from '@/shared/types/text-entity';

export const MEDIA_TYPES = ['PHOTO', 'VIDEO', 'GIF', 'DOCUMENT'] as const;

export const textEntityTypeSchema = z.enum([
  'BOLD',
  'ITALIC',
  'UNDERLINE',
  'STRIKETHROUGH',
  'SPOILER',
  'CODE',
  'PRE',
  'TEXT_LINK',
]);

export const textEntitySchema = z.object({
  type: textEntityTypeSchema,
  offset: z.number().int().min(0),
  length: z.number().int().min(1),
  url: z.string().url().optional(),
  language: z.string().optional(),
});

export const mediaTypeSchema = z.enum(MEDIA_TYPES);

export const mediaItemSchema = z.object({
  type: mediaTypeSchema,
  fileId: z.string().min(1),
  caption: z.string().optional(),
  url: z.string().url().optional(),
});

export const inlineButtonSchema = z.object({
  text: z.string().min(1),
  url: z.string().url(),
});

export const creativeDraftSchema = z.object({
  text: z.string().max(4096),
  entities: z.array(textEntitySchema),
  media: z.array(mediaItemSchema),
  buttons: z.array(inlineButtonSchema).max(5),
  disableWebPagePreview: z.boolean(),
});

export type CreativeDraft = z.infer<typeof creativeDraftSchema>;

export const creativeTemplateSchema = z.object({
  id: z.string(),
  title: z.string().min(1),
  draft: creativeDraftSchema,
  version: z.number().int().min(1),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type CreativeTemplate = z.infer<typeof creativeTemplateSchema>;

export const creativeVersionSchema = z.object({
  version: z.number().int().min(1),
  draft: creativeDraftSchema,
  createdAt: z.string(),
});

export type CreativeVersion = z.infer<typeof creativeVersionSchema>;

export function validateEntities(text: string, entities: Array<{ offset: number; length: number }>): boolean {
  return entities.every((e) => e.offset >= 0 && e.offset + e.length <= text.length);
}

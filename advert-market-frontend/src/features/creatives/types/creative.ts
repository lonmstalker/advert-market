import { z } from 'zod/v4';
import type { InlineButton, MediaItem, MediaType, TelegramKeyboardRow, TextEntity } from '@/shared/types/text-entity';
import { TextEntityType } from '@/shared/types/text-entity';

export type { InlineButton, MediaItem, MediaType, TelegramKeyboardRow, TextEntity };
export { TextEntityType };

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
  id: z.string().min(1),
  type: mediaTypeSchema,
  url: z.string().url(),
  thumbnailUrl: z.string().url().optional(),
  fileName: z.string().optional(),
  fileSize: z.string().optional(),
  mimeType: z.string().min(1),
  sizeBytes: z.number().int().min(0),
  caption: z.string().optional(),
});

export const inlineButtonSchema = z.object({
  id: z.string().min(1),
  text: z.string().min(1).max(50),
  url: z.string().url().optional(),
});

export const keyboardRowSchema = z.array(inlineButtonSchema).min(1).max(5);

export const creativeDraftSchema = z.object({
  text: z.string().max(4096),
  entities: z.array(textEntitySchema),
  media: z.array(mediaItemSchema).max(10),
  buttons: z.array(keyboardRowSchema).max(5),
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
  return entities.every((entity) => entity.offset >= 0 && entity.offset + entity.length <= text.length);
}

export function makeLocalId(prefix: string): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `${prefix}-${crypto.randomUUID()}`;
  }
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function toMediaTypeByMime(mimeType: string): MediaType {
  if (mimeType.startsWith('image/gif')) return 'GIF';
  if (mimeType.startsWith('image/')) return 'PHOTO';
  if (mimeType.startsWith('video/')) return 'VIDEO';
  return 'DOCUMENT';
}

export function countButtons(rows: TelegramKeyboardRow[]): number {
  return rows.reduce((sum, row) => sum + row.length, 0);
}

export function ensureButtonId(button: Omit<InlineButton, 'id'> & Partial<Pick<InlineButton, 'id'>>): InlineButton {
  return {
    id: button.id?.trim() ? button.id : makeLocalId('btn'),
    text: button.text,
    ...(button.url ? { url: button.url } : {}),
  };
}

export function ensureMediaDefaults(media: Partial<MediaItem> & Pick<MediaItem, 'type' | 'url'>): MediaItem {
  return {
    id: media.id?.trim() ? media.id : makeLocalId('media'),
    type: media.type,
    url: media.url,
    thumbnailUrl: media.thumbnailUrl,
    fileName: media.fileName,
    fileSize: media.fileSize,
    mimeType: media.mimeType ?? 'application/octet-stream',
    sizeBytes: media.sizeBytes ?? 0,
    caption: media.caption,
    fileId: media.fileId,
  };
}

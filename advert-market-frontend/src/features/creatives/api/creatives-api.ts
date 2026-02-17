import { z } from 'zod/v4';
import { api } from '@/shared/api/client';
import { type PaginatedResponse, paginatedResponseSchema } from '@/shared/api/types';
import type {
  CreativeTemplate,
  CreativeVersion,
  InlineButton,
  MediaItem,
  MediaType,
  TelegramKeyboardRow,
  TextEntity,
} from '../types/creative';
import { ensureButtonId, ensureMediaDefaults, makeLocalId, mediaTypeSchema } from '../types/creative';

const apiTextEntitySchema = z.object({
  type: z.enum(['BOLD', 'ITALIC', 'UNDERLINE', 'STRIKETHROUGH', 'SPOILER', 'CODE', 'PRE', 'TEXT_LINK']),
  offset: z.number().int().min(0),
  length: z.number().int().min(1),
  url: z.string().url().nullable().optional(),
  language: z.string().nullable().optional(),
});

const apiInlineButtonSchema = z.object({
  id: z.string().nullable().optional(),
  text: z.string().min(1).max(50),
  url: z.string().max(2048).nullable().optional(),
});

const apiMediaAssetSchema = z.object({
  id: z.string().nullable().optional(),
  type: mediaTypeSchema,
  url: z.string().url(),
  thumbnailUrl: z.string().url().nullable().optional(),
  fileName: z.string().nullable().optional(),
  fileSize: z.string().nullable().optional(),
  mimeType: z.string().nullable().optional(),
  sizeBytes: z.number().int().min(0).nullable().optional(),
  caption: z.string().nullable().optional(),
});

const apiDraftSchema = z.object({
  text: z.string().max(4096),
  entities: z.array(apiTextEntitySchema),
  media: z.array(apiMediaAssetSchema).max(10),
  keyboardRows: z.array(z.array(apiInlineButtonSchema).min(1).max(5)).max(5),
  disableWebPagePreview: z.boolean(),
});

const apiTemplateSchema = z.object({
  id: z.string(),
  title: z.string().min(1),
  draft: apiDraftSchema,
  version: z.number().int().min(1),
  createdAt: z.string(),
  updatedAt: z.string(),
});

const apiVersionSchema = z.object({
  version: z.number().int().min(1),
  draft: apiDraftSchema,
  createdAt: z.string(),
});

export type CreateCreativeRequest = {
  title: string;
  text: string;
  entities: TextEntity[];
  media: MediaItem[];
  buttons: TelegramKeyboardRow[];
  disableWebPagePreview: boolean;
};

export type UpdateCreativeRequest = CreateCreativeRequest;

type ApiCreateCreativeRequest = {
  title: string;
  text: string;
  entities: TextEntity[];
  media: MediaItem[];
  keyboardRows: TelegramKeyboardRow[];
  disableWebPagePreview: boolean;
};

type ApiDraft = z.infer<typeof apiDraftSchema>;
type ApiTemplate = z.infer<typeof apiTemplateSchema>;
type ApiVersion = z.infer<typeof apiVersionSchema>;

export function fetchCreatives(params?: {
  cursor?: string;
  limit?: number;
}): Promise<PaginatedResponse<CreativeTemplate>> {
  return api
    .get('/creatives', {
      schema: paginatedResponseSchema(apiTemplateSchema),
      params: {
        cursor: params?.cursor,
        limit: params?.limit ?? 20,
      },
    })
    .then((page) => ({
      ...page,
      items: page.items.map(mapApiTemplate),
    }));
}

export function fetchCreative(id: string): Promise<CreativeTemplate> {
  return api.get(`/creatives/${id}`, { schema: apiTemplateSchema }).then(mapApiTemplate);
}

export function createCreative(req: CreateCreativeRequest): Promise<CreativeTemplate> {
  return api.post('/creatives', toApiRequest(req), { schema: apiTemplateSchema }).then(mapApiTemplate);
}

export function updateCreative(id: string, req: UpdateCreativeRequest): Promise<CreativeTemplate> {
  return api.put(`/creatives/${id}`, toApiRequest(req), { schema: apiTemplateSchema }).then(mapApiTemplate);
}

export function deleteCreative(id: string): Promise<void> {
  return api.delete(`/creatives/${id}`);
}

export function fetchCreativeVersions(id: string): Promise<CreativeVersion[]> {
  return api
    .get(`/creatives/${id}/versions`, {
      schema: apiVersionSchema.array(),
    })
    .then((versions) => versions.map(mapApiVersion));
}

export function uploadCreativeMedia(file: File, mediaType: MediaType, caption?: string): Promise<MediaItem> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('mediaType', mediaType);
  if (caption) {
    formData.append('caption', caption);
  }
  return api.postForm('/creatives/media', formData, { schema: apiMediaAssetSchema }).then(mapApiMediaAsset);
}

export function deleteCreativeMedia(mediaId: string): Promise<void> {
  return api.delete(`/creatives/media/${mediaId}`);
}

function toApiRequest(req: CreateCreativeRequest): ApiCreateCreativeRequest {
  return {
    title: req.title,
    text: req.text,
    entities: req.entities,
    media: req.media.map((media) => ensureMediaDefaults(media)),
    keyboardRows: req.buttons.map((row) => row.map((button) => ensureButtonId(button))),
    disableWebPagePreview: req.disableWebPagePreview,
  };
}

function mapApiTemplate(template: ApiTemplate): CreativeTemplate {
  return {
    id: template.id,
    title: template.title,
    draft: mapApiDraft(template.draft),
    version: template.version,
    createdAt: template.createdAt,
    updatedAt: template.updatedAt,
  };
}

function mapApiVersion(version: ApiVersion): CreativeVersion {
  return {
    version: version.version,
    draft: mapApiDraft(version.draft),
    createdAt: version.createdAt,
  };
}

function mapApiDraft(draft: ApiDraft) {
  return {
    text: draft.text,
    entities: draft.entities.map((entity) => ({
      type: entity.type,
      offset: entity.offset,
      length: entity.length,
      ...(entity.url ? { url: entity.url } : {}),
      ...(entity.language ? { language: entity.language } : {}),
    })),
    media: draft.media.map(mapApiMediaAsset),
    buttons: draft.keyboardRows.map(mapApiKeyboardRow),
    disableWebPagePreview: draft.disableWebPagePreview,
  };
}

function mapApiKeyboardRow(row: z.infer<typeof apiInlineButtonSchema>[]): TelegramKeyboardRow {
  return row.map((button) =>
    ensureButtonId({
      id: button.id ?? undefined,
      text: button.text,
      url: button.url ?? undefined,
    }),
  );
}

function mapApiMediaAsset(media: z.infer<typeof apiMediaAssetSchema>): MediaItem {
  return ensureMediaDefaults({
    id: media.id ?? makeLocalId('media'),
    type: media.type,
    url: media.url,
    thumbnailUrl: media.thumbnailUrl ?? undefined,
    fileName: media.fileName ?? undefined,
    fileSize: media.fileSize ?? undefined,
    mimeType: media.mimeType ?? 'application/octet-stream',
    sizeBytes: media.sizeBytes ?? 0,
    caption: media.caption ?? undefined,
  });
}

export function nonEmptyKeyboardRows(rows: TelegramKeyboardRow[]): TelegramKeyboardRow[] {
  return rows
    .map((row) =>
      row
        .map((button) => ensureButtonId(button))
        .filter((button) => button.text.trim().length > 0 && Boolean(button.url)),
    )
    .filter((row) => row.length > 0);
}

export function flattenButtons(rows: TelegramKeyboardRow[]): InlineButton[] {
  return rows.flat();
}

import { api } from '@/shared/api/client';
import { type PaginatedResponse, paginatedResponseSchema } from '@/shared/api/types';
import {
  type CreativeTemplate,
  type CreativeVersion,
  creativeTemplateSchema,
  creativeVersionSchema,
} from '../types/creative';

export type CreateCreativeRequest = {
  title: string;
  text: string;
  entities: Array<{ type: string; offset: number; length: number; url?: string; language?: string }>;
  media: Array<{ type: string; fileId: string; caption?: string }>;
  buttons: Array<{ text: string; url: string }>;
  disableWebPagePreview: boolean;
};

export type UpdateCreativeRequest = CreateCreativeRequest;

export function fetchCreatives(params?: {
  cursor?: string;
  limit?: number;
}): Promise<PaginatedResponse<CreativeTemplate>> {
  return api.get('/creatives', {
    schema: paginatedResponseSchema(creativeTemplateSchema),
    params: {
      cursor: params?.cursor,
      limit: params?.limit ?? 20,
    },
  });
}

export function fetchCreative(id: string): Promise<CreativeTemplate> {
  return api.get(`/creatives/${id}`, { schema: creativeTemplateSchema });
}

export function createCreative(req: CreateCreativeRequest): Promise<CreativeTemplate> {
  return api.post('/creatives', req, { schema: creativeTemplateSchema });
}

export function updateCreative(id: string, req: UpdateCreativeRequest): Promise<CreativeTemplate> {
  return api.put(`/creatives/${id}`, req, { schema: creativeTemplateSchema });
}

export function deleteCreative(id: string): Promise<void> {
  return api.delete(`/creatives/${id}`);
}

export function fetchCreativeVersions(id: string): Promise<CreativeVersion[]> {
  return api.get(`/creatives/${id}/versions`, {
    schema: creativeVersionSchema.array(),
  });
}
